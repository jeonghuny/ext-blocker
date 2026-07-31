package com.jeonghuny.ext_blocker.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 파일 앞부분 바이트로 실행 파일 여부를 판정한다.
 *
 * 한계(의도적으로 좁게 설계함):
 *  - docx/xlsx/pptx/jar/apk는 모두 ZIP(PK\x03\x04)이므로 시그니처만으로 구분 불가
 *  - txt/csv/svg 등은 시그니처가 없음
 *  → 따라서 "허용 타입 판별"이 아니라 "명백한 실행 파일 탐지"만 수행한다.
 */
public class FileSignatureInspector {

    private record Signature(String label, byte[] magic) { }

    private static final List<Signature> EXECUTABLES = List.of(
            new Signature("Windows 실행 파일(PE)",  new byte[]{'M', 'Z'}),
            new Signature("Linux 실행 파일(ELF)",   new byte[]{0x7F, 'E', 'L', 'F'}),
            new Signature("macOS 실행 파일(Mach-O)", new byte[]{(byte)0xCF, (byte)0xFA, (byte)0xED, (byte)0xFE}),
            new Signature("macOS 실행 파일(Mach-O)", new byte[]{(byte)0xCE, (byte)0xFA, (byte)0xED, (byte)0xFE}),
            new Signature("Java 클래스 파일",        new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE}),
            new Signature("스크립트(shebang)",       new byte[]{'#', '!'})
    );

    private static final int PEEK = 8;

    /** 실행 파일이면 그 설명을, 아니면 null을 반환한다. */
    public String detectExecutable(InputStream in) throws IOException {
        byte[] head = in.readNBytes(PEEK);
        for (Signature sig : EXECUTABLES) {
            if (startsWith(head, sig.magic())) return sig.label();
        }
        return null;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}