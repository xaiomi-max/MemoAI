# 讯飞 SparkChain SDK

已集成 `SparkChain.aar` + `Codec.aar`。

在 `local.properties` 配置（从讯飞控制台获取）：

```
IFLYTEK_APP_ID=3d0f8f93
IFLYTEK_API_KEY=你的APIKey
IFLYTEK_API_SECRET=你的APISecret
```

配置后重新编译安装。首页右侧麦克风长按即可使用 SparkChain 听写。

若未配置 APIKey/Secret，会回退到 WebAPI 路径（同样需密钥）。

文档：https://www.xfyun.cn/doc/asr/voicedictation/SparkChain-Android-SDK.html
