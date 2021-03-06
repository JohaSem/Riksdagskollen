package oscar.riksdagskollen.Util.Helper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

public interface AnalyticsWrapper {

    void initAnalytics(Context context);

    void setCurrentScreen(Activity activity, String screenName);

    void log(String logMessage);

    void logEvent(String event, @Nullable Bundle bundle);

}
