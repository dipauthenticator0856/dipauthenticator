package org.shadowice.flocke.andotp.Services.API.Models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DipDeviceDetailModel extends RealmObject {

    @PrimaryKey
    @SerializedName("desecret")
    @Expose
    private String desecret;
    @SerializedName("digits")
    @Expose
    private String digits;
    @SerializedName("label")
    @Expose
    private String label;
    @SerializedName("photoid")
    @Expose
    private String photoid;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPhotoid() {
        return photoid;
    }

    public void setPhotoid(String photoid) {
        this.photoid = photoid;
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
