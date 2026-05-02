# 第 2 阶段计划：本地存储与持久化

目标：把当前 mock app 从“内存态可点击 demo”推进到“本地可保存的 MVP 骨架”。完成后，用户创建记录、修改设置、收藏图页、打开时间线和纪念册时，数据应来自本地持久化，而不是每次启动都重置。

## 1. Android 数据层

- 引入 Room，建立本地数据库。
- 建立实体：`DiaryEntryEntity`、`DiarySlideEntity`、`DiaryNoteEntity`、`EntryMediaEntity`。
- 建立 DAO：
  - 创建/读取日记记录。
  - 按日期读取时间线。
  - 更新 slide 收藏状态。
  - 读取纪念册收藏图页。
- 保留当前 `domain/model`，新增 entity 与 domain mapper，避免 UI 直接依赖 Room entity。

## 2. 设置持久化

- 引入 DataStore Preferences。
- 持久化：
  - 用户名。
  - 预产期。
  - 当前主题。
- 当前 `SettingsScreen` UI 不改视觉，只把内存态改为持久化状态。

## 3. Mock 生成落库

- Record 提交后，先继续使用现有 mock 生成结果。
- 将生成出来的 `DiaryEntry / slides / notes` 写入 Room。
- 自动进入 Result，并让 Timeline 能读到新记录。
- 收藏按钮更新 Room 中的收藏状态，Memory Book 从数据库读取收藏图页。

## 4. 本地图片预留

- 本阶段先不做真实 Photo Picker。
- 预留 `EntryMediaEntity` 和 repository 接口。
- 结果页继续使用渐变 mock 图卡，后续第 3/5 阶段再接入真实图片选择、保存和渲染。

## 5. 架构调整

- 新增 `data/local` 包放 Room database、DAO、entity。
- 新增 `data/settings` 包放 DataStore。
- 新增 repository 层，例如 `DiaryRepository`、`SettingsRepository`。
- 新增简单 ViewModel 或状态持有层，让 `YouAndMeDiaryApp` 不再直接管理所有业务状态。
- 暂不引入 Navigation Compose，除非状态迁移时发现当前 enum route 明显阻碍页面参数管理。

## 6. 测试计划

- Room DAO 单元/仪器测试：
  - 插入日记后可按日期读取。
  - 收藏状态可更新。
  - 纪念册只返回收藏 slide。
- DataStore 测试：
  - 用户名、预产期、主题能写入和读取。
- Repository 测试：
  - mock 记录落库后可被 timeline/result/memory 查询。
- 手动真机验收：
  - 创建一条记录后重启 App，时间线仍可看到。
  - 收藏图页后重启 App，纪念册仍可看到。
  - 设置用户名和主题后重启 App 仍保留。

## 7. 暂不做

- 真实图片选择和相册权限。
- 语音输入。
- Gemma API。
- 分享长图导出。
- Room migration 复杂测试。
- 云同步或账号系统。
