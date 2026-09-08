package com.marcos.leairning.documents;

import lombok.Getter;

@Getter
public enum MimeTypes {
    PDF("application/pdf"),
    DOC("application/msword"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    TXT("text/plain"),
    CSV("text/csv"),
    MD("text/markdown");

    private final String valor;

    MimeTypes(String valor) {
        this.valor = valor;
    }

    public static Boolean isValid(String mimeType) {
        if (mimeType == null) return false;
        for (MimeTypes mimeTypes : MimeTypes.values()) {
            if (mimeTypes.getValor().equals(mimeType)) return true;
        }
        return false;
    }
}
