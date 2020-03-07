package org.shadowice.flocke.andotp.Services.API.Models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DipDigitModel {

    @SerializedName("success")
    @Expose
    private String success;
    @SerializedName("posts")
    @Expose
    private Posts posts;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public Posts getPosts() {
        return posts;
    }

    public void setPosts(Posts posts) {
        this.posts = posts;
    }

    public class Posts {

        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("desecret")
        @Expose
        private String desecret;
        @SerializedName("digits")
        @Expose
        private String digits;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDesecret() {
            return desecret;
        }

        public void setDesecret(String desecret) {
            this.desecret = desecret;
        }

        public String getDigits() {
            return digits;
        }

        public void setDigits(String digits) {
            this.digits = digits;
        }
    }
}
