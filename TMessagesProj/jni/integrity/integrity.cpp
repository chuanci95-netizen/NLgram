#include "meth.h"
#include "openat.h"
#include "read_cert.h"
#include "SHA1.h"

static const char *SIGN = "3A0F57FE06485D0B90D0ACD990E3A30328E3988D";

extern "C" {
int verifySign(JNIEnv *env) {
    // ★魔改(NLgram 2026-09-13): 绕过官方签名校验 = 修"点开秒闪退"真凶
    //   原逻辑: 读APK证书→SHA1→对比硬编码官方签名SIGN, 不匹配就返JNI_ERR
    //   → JNI_OnLoad返JNI_ERR → System.loadLibrary(tmessages)抛异常 → native库加载不了 → ApplicationLoader抛"can't load native libraries" → 秒闪退
    //   我们用AGP debug keystore自签, 证书SHA1必然≠官方SIGN, 所以每次必失败. 直接恒返JNI_OK, 放行任何签名.
    (void) env;
    return JNI_OK;
}
}
