package jobt.plugin.mavenresolver;

import java.io.Serializable;

public class CacheWrapper<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;
    private final T data;
    private String signature;

    public CacheWrapper(final String signature, final T data) {
        this.signature = signature;
        this.data = data;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(final String signature) {
        this.signature = signature;
    }

    public T getData() {
        return data;
    }

}
