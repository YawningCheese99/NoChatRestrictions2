package test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class ProxyAgent implements ClassFileTransformer {
    public static void premain(String agentArgs, Instrumentation inst) {
        log("Starting...");
        inst.addTransformer(new ProxyAgent());
    }

    public static void log(String msg) {
        System.out.println("[NoChatRestrictions] " + msg);
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className != null && className.startsWith("com/mojang/authlib/yggdrasil/Y")) {
            classfileBuffer = replace(classfileBuffer, "/player/attributes");
            classfileBuffer = replace(classfileBuffer, "/privileges");
            return classfileBuffer;
        }
        return null;
    }

    public static byte[] replace(byte[] buffer, String s) {
        byte[] target = s.getBytes();
        byte[] replacement = ("7" + s.substring(0, s.length() - 1)).getBytes();
        
        for (int i = 0; i <= buffer.length - target.length; i++) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (buffer[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                System.arraycopy(replacement, 0, buffer, i, replacement.length);
                log("Code modification successful! Replaced " + s);
            }
        }
        return buffer;
    }
}
