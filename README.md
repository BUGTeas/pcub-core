# 盘灵互通核心组件

## 📖 简介

依「盘灵古域」系列内容适配互通的功能需求，扩展 Geyser 的功能




## ⚙️ 功能

### 实现药水根据颜色映射为自定义物品
- 需要通过 Geyser 为物品定义自定义模型映射，其中包含一个专用谓词：匹配 `custom_model_data` 组件（下称 CMD）中的 `pcubc_potion_color_<药水颜色>` 字符串，其中药水颜色为 MC 格式 `R*255*255 + G*255 + B`
- 当物品实例带有药水颜色属性，系统会匹配此物品的 ID 下，专用谓词此颜色最接近的自定义物品映射 (欧式距离算法，含加权)
  - 比如游戏内有以下物品实例:
    - 药水 1，颜色为 #F82423: `potion[potion_contents={custom_color:16262179}]`
    - 药水 2，颜色为 #87A363: `potion[potion_contents={custom_color:8889187}]`
  - 定义以下自定义物品映射:
    - 自定义 1，包含专用谓词 (颜色为 #4E9331)：
      ```json
      { "type": "match", "property": "custom_model_data",
        "value": "pcubc_potion_color_5149489" }
      ```
    - 自定义 2，包含专用谓词 (颜色为 #F82423)：
      ```json
      { "type": "match", "property": "custom_model_data",
        "value": "pcubc_potion_color_16262179" }
      ```
  - 效果
    - 药水 1 -> 自定义 2 (颜色 #F82423 完全匹配)
    - 药水 2 -> 自定义 1 (和 #87A363 最相似的颜色是 #4E9331，而非 #F82423)
- 专用谓词**不应由真实物品的 CMD 所满足**，当此映射项的其它条件满足且匹配为最相似颜色时，物品数据会被在网络数据包层面临时修改，伪造在 Geyser 默认谓词规则下能满足的 CMD 从而应用映射，此过程通常不会影响真实物品数据
- 若专用谓词存在以下情况，会导致映射不能正常匹配：
  - 专用谓词的索引（`index`）小于任何一个匹配 CMD 中字符串的普通谓词的索引
  - 索引超出真实物品中 CMD 字符串列表的长度（**刚好相等除外**，真实数据无 CMD 则视作长度为 0）
  - 使用 Geyser API 将专用谓词反向（即 `ItemMatchPredicate.customModelData(index, "pcubc_potion_color_<药水颜色>").negate()`）或嵌套在其它谓词内（即 `MinecraftPredicate.and` 或 `MinecraftPredicate.or`）




## 💿 适用环境

- 基于 Bukkit 的服务端 (如Spigot、Paper)
- Velocity 代理 (WIP)
- Geyser 独立版 (作为 Geyser 扩展使用)

所有平台均使用相同的 Jar 包




## 🔨 构建

- 使用 Gradle 构建: 项目目录下使用 `./gradlew build`
- 成品 Jar: `build/libs/PCUB-Core.jar`

有关 geyser 子项目 (包括在其它子项目内的): 其中的代码通常涉及到 Geyser 的底层，而 Geyser 在不同平台下所使用的部分依赖被 relocate 重定位，故这里也需要针对不同平台做不同处理。




## 📜 即将实现

- 特殊物品映射 API
  - 基于完整物品数据的自定义映射条件，以及相似度匹配（类似目前药水颜色映射）
  - 自定义物品属性（基岩版组件）
- 类似插件的资源管理 (实现 Geyser 不支持的模块化语言文件，和非编程手段无法实现的资源顺序管理)
  - 基岩端资源包
  - 自定义物品映射 JSON
  - 语言文件
  - form 表单文件 (免编程制作表单界面)




## 💡 盘灵无界交流社区

反馈问题、DLC 支持、茶水闲聊：

- [腾讯频道](https://pd.qq.com/s/v8t170qb)
- [KOOK](https://kook.vip/KJ7Zlx)