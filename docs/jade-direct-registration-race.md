# Jade 直连注册：白名单与竞态崩溃问题说明（含 visualmana 移植指南）

本文档记录 Ars Mana HUD（arsmanahud, Forge 1.20.1）在 **GregTech Odyssey** 整合包（`gtocore` 0.5.6-beta）中遇到的 Jade 相关问题的完整分析与修复方案。**Visual Mana（visualmana）存在完全相同的两处问题与相同的修复方式**，本文档可直接作为移植依据。

## 1. 背景

- 整合包核心模组 **GTO Core（gtocore）** 通过 Mixin 对 Jade 的 `CommonProxy.loadComplete`（`FMLLoadCompleteEvent` 处理器）做了 **`@Overwrite` 整体替换**（反编译确认：`com.gtocore.mixin.jade.CommonProxyMixin`，`gtocore.mixins.json` 中 `jade.CommonProxyMixin`）。
- 替换后的 `loadComplete` 不再扫描各模组的 `@WailaPlugin` 注解，而是**硬编码遍历 7 个插件**：
  `VanillaPlugin`、`UniversalPlugin`、`CorePlugin`、AE2 `JadeModule`、`GTOJadePlugin`（gtocore 自带约 21 个 provider）、Apotheosis `AdventureHwylaPlugin`、`EnchHwylaPlugin`，然后依次调用 `plugin.register(WailaCommonRegistration.INSTANCE)` 与 `registerClient(WailaClientRegistration.INSTANCE)`。
- 后果 1：任何模组的 `@WailaPlugin` 插件类都不会被加载 → **Jade 上的自定义显示行全部失效**（arsmanahud 的功能 6、visualmana 的魔力/沙漏行均不显示）。
- 后果 2（本修复重点）：多个模组为绕过白名单各自"直接注册"，在并行阶段并发写入 Jade 的非线程安全内部单例，**偶发触发 fastutil 崩溃**。

## 2. 问题一：插件扫描被白名单替换 → 功能不显示

### 原实现（失效）

```java
@WailaPlugin(ArsManaHud.MODID)
public class ArsManaHudJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ContainerAmountProvider.INSTANCE, SourceJar.class);
        // ...
    }
}
```

### 修复：直接注册到 Jade 客户端注册单例（保留注解路径作双保险）

```java
public final class JadeDirectRegistration {
    private JadeDirectRegistration() {}

    /** Must only run on the physical client. No-op when Jade is not installed. */
    public static void registerIfPresent() {
        if (!ModList.get().isLoaded("jade")) {
            return;
        }
        WailaClientRegistration.INSTANCE.registerBlockComponent(ContainerAmountProvider.INSTANCE, SourceJar.class);
        // ... 与插件里相同的一组 registerBlockComponent 调用
    }
}
```

> 注意：`snownee.jade.impl.WailaClientRegistration` 是 Jade 的**内部实现类**（非公开 API）。仅为绕过白名单而使用；该单例与类名在 Jade 11.x（11.13.2+）范围内稳定。

## 3. 问题二：直连注册的并发竞态 → fastutil 崩溃（本次修复重点）

### 崩溃现象

- 报错模组：**Jade**；时间点：加载完成后、进入游戏前（`FMLLoadCompleteEvent` 阶段）。
- 报错：`java.lang.ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 33`。
- 栈（截取关键帧）：

```
it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap.rehash(:1853)   <- 读取 key[-1]
it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap.insert(:306)
it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap.put(:314)
snownee.jade.impl.PriorityStore.put(:65)
snownee.jade.impl.PriorityStore.put(:58)
snownee.jade.impl.HierarchyLookup.register(:43)
snownee.jade.impl.WailaClientRegistration.registerBlockComponent(:149)
snownee.jade.addon.vanilla.VanillaPlugin.registerClient(:163 或 :165)
snownee.jade.util.CommonProxy.loadComplete(:567)  <- 已被 gtocore CommonProxyMixin 覆盖
```

### 根因

1. Jade 的 `PriorityStore` 内部持有 `new Object2IntLinkedOpenHashMap<>()`（**默认容量 16 → 数组长度 33**，`maxFill = 24`，第 25 次 `put` 触发 `rehash`）。
2. 该 map **非线程安全**，但 `WailaCommonRegistration.INSTANCE.priorities` 是进程级共享单例，注册路径 `registerBlockComponent → HierarchyLookup.register → PriorityStore.put` 都会写它。
3. **Forge 将模组生命周期事件（`FMLClientSetupEvent`、`FMLLoadCompleteEvent`）对各个模组并行派发**（`ModWorkManager.parallelExecutor()` = 专用 ForkJoinPool，已用 fmlcore 字节码确认）。
4. arsmanahud 0.4.2 与 visualmana 0.1.4 都在各自的 `FMLClientSetupEvent` 处理器里直接调用 `registerIfPresent()` —— **两个处理器跑在不同的 ForkJoinPool worker 上，同时写同一张 fastutil map** → 链表元数据（`first`/`last`/`link`/`size`）被撕裂写坏。
5. 损坏当时不报错，直到 `COMPLETE` 阶段 gtocore 的白名单循环注册 `VanillaPlugin` 的 provider，第 25 次 `put` 触发 `rehash`，遍历到断链（`link[i] == -1`）→ 读 `key[-1]` → 崩溃。
6. 两次崩溃分别落在 `VanillaPlugin.java:163`（`HarvestToolProvider`）与 `:165`（`EnchantmentPowerProvider`）——相邻注册项、行号漂移，证明失败前 map 的条目数随每次竞态结果变化。

### 为什么"第一次崩、第二次就好"

纯竞态。Jar 变更/冷启动会改变类加载与线程调度时序，撞车窗口变大；热启动时序稳定后两写入者不再重叠。同一套 mods 曾在多个会话正常、偶发崩溃两次，符合竞态特征而非确定性缺陷。

### 为什么加装 arsmanahud 0.4.2 后才出现

此前该整合包里只有 visualmana 一个模组在客户端设置阶段直连注册（单写入者，无并发）；arsmanahud 0.4.2 加入后变成**两个并发写入者**，竞态成立。

## 4. 修复方案：`enqueueWork` 串行化（方向 1）

`FMLClientSetupEvent.enqueueWork(Runnable)` 会把任务放进 **SIDED_SETUP 阶段的同步工作队列，在所有模组的客户端设置处理器全部结束后、由渲染线程逐个串行执行**，之后再进入 `COMPLETE` 阶段。由此：

- 我们的注册**不再与其它模组的客户端设置处理器（如 visualmana 的直连注册）并发**；
- 仍然**早于** `FMLLoadCompleteEvent`（gtocore 白名单循环、Jade 自身的 provider 收集），功能正常；
- 队列内本身只有我们的一个任务，且 `COMPLETE` 阶段在其之后，因此该 map 全程只剩单写入者。

### arsmanahud 已采用的写法（`ManaHudClient.java`）

```java
@SubscribeEvent
public static void onClientSetup(FMLClientSetupEvent event) {
    // enqueueWork defers the direct Jade registration to the render thread,
    // running AFTER every mod's client-setup handler has finished. Jade's
    // registration singleton is not thread-safe, and other mods (e.g. visualmana)
    // register directly during the parallel client-setup dispatch; serializing
    // our writes prevents concurrent corruption of Jade's PriorityStore map.
    event.enqueueWork(JadeDirectRegistration::registerIfPresent);
}
```

## 5. visualmana 需要做的改动（照抄即可）

`VisualMana.java` 中：

```java
private static void onClientSetup(FMLClientSetupEvent event) {
    // BEFORE（并发写入，与其它模组的直连注册竞态，偶发使 Jade 的
    // PriorityStore 内部 fastutil map 损坏而崩溃）：
    //   JadeDirectRegistration.registerIfPresent();
    //
    // AFTER（渲染线程串行执行，晚于所有模组的客户端设置处理器，
    // 早于 FMLLoadCompleteEvent 的插件注册循环）：
    event.enqueueWork(JadeDirectRegistration::registerIfPresent);
}
```

visualmana 的 `JadeDirectRegistration`、`JadeManaProvider`（带 tag 的 remove-then-add 工具提示行）无需改动。

## 6. 注意事项

- **不要**在直连注册外加 `synchronized` 或自建锁：Jade 内部的写入没有使用同一把锁，锁不住其它模组，反而引入死锁风险。串行化时机（enqueueWork）才是治本。
- **保留** `@WailaPlugin` 注解路径：正常整合包中双路径注册同一 provider 是安全的——provider 的工具提示行必须用 `tooltip.remove(uid)` + `tooltip.add(component, uid)`（带 tag）模式写入，重复调用时去重（arsmanahud 的 `ContainerAmountProvider` 与 visualmana 的 `JadeManaProvider` 均已如此）。
- **Jade 未安装时**：`ModList.get().isLoaded("jade")` 守卫必须位于任何 Jade 类引用之前（Java 惰性类加载保证不会 `NoClassDefFoundError`），且直连注册只在物理客户端调用。
- 若之后更换/升级整合包里的 gtocore，重新确认其 `CommonProxyMixin` 是否仍在（白名单机制变化会同时影响两处行为：功能是否显示、是否需要继续直连注册）。
- 建议两个模组同步发版，避免"只有一个模组修了"导致竞态窗口依旧存在。

## 7. 参考信息

- 崩溃会话日志：`logs/debug-*.log.gz`（`Failed to complete lifecycle event COMPLETE`、`Caught exception during event FMLLoadCompleteEvent dispatch for modid jade`）。
- 崩溃报告：`crash-reports/crash-*-fml.txt`（`-- MOD jade --` 段）。
- Jade 版本：11.13.2+forge（`Jade-1.20.1-Forge-11.13.2.jar`）；fastutil 为 MC 自带 8.5.9。
- gtocore 版本：`gtocore-forge-1.20.1-0.5.6-beta.jar`。
- 关联提交（arsmanahud）：`af4116b`（直连注册）、`7f88cc4`（enqueueWork 串行化修复）。