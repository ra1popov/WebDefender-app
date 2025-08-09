package app.ui.main.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import app.R;
import app.common.Utils;
import app.preferences.model.Stat;
import app.ui.main.widget.icons.StatIconNoadsView;
import app.ui.main.widget.icons.StatIconThreatView;
import app.ui.main.widget.icons.StatIconTrafficView;

public class StatView extends LinearLayout {

    private Context context;
    private TextView tvStatAd;
    private LinearLayout llStatAdblock;
    private TextView tvStatTraffic;
    private TextView tvStatThreat;
    private TextView tvStatTrafficText;
    private TextView tvStatAdblockText;
    private TextView tvStatThreatText;
    private StatIconTrafficView sitvStatIconTraffic;
    private StatIconNoadsView sitvStatIconNoads;
    private StatIconThreatView sitvStatIconThreat;

    public StatView(Context context) {
        super(context);
        init(context);
    }

    public StatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_stat, this);

        tvStatAd = findViewById(R.id.widget_stat_block);
        llStatAdblock = findViewById(R.id.widget_stat_adblock);
        tvStatTraffic = findViewById(R.id.widget_stat_traffic);
        tvStatThreat = findViewById(R.id.widget_stat_threat);

        tvStatTrafficText = findViewById(R.id.widget_stat_traffic_text);
        tvStatAdblockText = findViewById(R.id.widget_stat_adblock_text);
        tvStatThreatText = findViewById(R.id.widget_stat_threat_text);

        sitvStatIconTraffic = findViewById(R.id.widget_stat_icon_traffic);
        sitvStatIconNoads = findViewById(R.id.widget_stat_icon_noads);
        sitvStatIconThreat = findViewById(R.id.widget_stat_icon_threat);

        showAdBlock(false);

        // Loading statistics

        tvStatAd.setText("0");
        tvStatTraffic.setText("0");
        tvStatThreat.setText("0");
    }

    public void setData(Stat stat) {
        // return array with minimal stats data (ads count, malware count, traffic saved in human form)

        int ad = stat.blockedAdsIpCount + stat.blockedAdsUrlCount;
        int threat = stat.blockedApkCount + stat.blockedMalwareSiteCount + stat.blockedPaidSiteCount;
        long trafficSaved = ad * 51200L + stat.proxyCompressionSave;
        String traffic = Utils.byteCountToHuman(trafficSaved);

        tvStatAd.setText(String.valueOf(ad));
        tvStatThreat.setText(String.valueOf(threat));
        tvStatTraffic.setText(traffic);

        // fake for screenshots
        // tvStatAd.setText("9353");
        // tvStatThreat.setText("1");
        // tvStatTraffic.setText("4,8 GB");
    }

    public void showAdBlock(boolean show) {
        if (show) {
            llStatAdblock.setVisibility(VISIBLE);
        } else {
            llStatAdblock.setVisibility(GONE);
        }
    }

    public void setPower(boolean power) {
        if (power) {
            sitvStatIconTraffic.setPowerOn();
            sitvStatIconNoads.setPowerOn();
            sitvStatIconThreat.setPowerOn();

            setTint(tvStatTraffic, R.color.widget_stat_traffic_text_color);
            setTint(tvStatAd, R.color.widget_stat_noads_text_color);
            setTint(tvStatThreat, R.color.widget_stat_threat_text_color);

            setTint(tvStatTrafficText, R.color.widget_stat_traffic_text_color);
            setTint(tvStatAdblockText, R.color.widget_stat_noads_text_color);
            setTint(tvStatThreatText, R.color.widget_stat_threat_text_color);
        } else {
            sitvStatIconTraffic.setPowerOff();
            sitvStatIconNoads.setPowerOff();
            sitvStatIconThreat.setPowerOff();

            setTint(tvStatTraffic, R.color.widget_stat_off_text_color);
            setTint(tvStatAd, R.color.widget_stat_off_text_color);
            setTint(tvStatThreat, R.color.widget_stat_off_text_color);

            setTint(tvStatTrafficText, R.color.widget_stat_off_text_color);
            setTint(tvStatAdblockText, R.color.widget_stat_off_text_color);
            setTint(tvStatThreatText, R.color.widget_stat_off_text_color);
        }
    }

    private void setTint(@NonNull TextView view, @ColorRes int id) {
        view.setTextColor(ContextCompat.getColor(context, id));
    }

}
