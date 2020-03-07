package org.shadowice.flocke.andotp.Common.Network.Rest;

public class RestCallback {

    public interface CommonInfoDelegate<T> {

        public void CallDidSuccess(T info);

        public void CallFailedWithError(String error);

    }

    public interface Permissions {
        public void Granted();

        public void Denied();

        public void NeverAskAgain();
    }

    public interface CommonDelegate1 {
        public void CallDidSuccess(String msg);

        public void CallFailedWithError(String error);
    }


}
