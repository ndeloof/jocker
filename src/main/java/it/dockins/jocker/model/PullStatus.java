package it.dockins.jocker.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PullStatus {

    @SerializedName("status")
    private String status;

    @SerializedName("id")
    private String id;

    @SerializedName("progress")
    private String progress;

    @SerializedName("progressDetail")
    private ProgressDetail progressDetail;

    public static class ProgressDetail {

        @SerializedName("current")
        private int current;

        @SerializedName("total")
        private int total;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(id).append(": ");
        sb.append(status);
        if (progress != null) sb.append(' ').append(progress);
        return sb.toString();
    }
}
