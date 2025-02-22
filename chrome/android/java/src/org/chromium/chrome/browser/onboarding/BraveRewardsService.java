package org.chromium.chrome.browser.onboarding;

import android.content.Intent;
import android.content.Context;
import android.app.Service;
import android.os.IBinder;
import android.util.Log;

import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.BraveRewardsNativeWorker;
import org.chromium.chrome.browser.BraveAdsNativeHelper;
import org.chromium.chrome.browser.BraveRewardsObserver;

public class BraveRewardsService extends Service implements BraveRewardsObserver{

    private static String TAG = "BraveRewardsService";

    private Context context;

    private BraveRewardsNativeWorker mBraveRewardsNativeWorker;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        this.context = this;
    }

    @Override
    public void onDestroy() {
        if (mBraveRewardsNativeWorker != null) {
            mBraveRewardsNativeWorker.RemoveObserver(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            mBraveRewardsNativeWorker = BraveRewardsNativeWorker.getInstance();
            if (mBraveRewardsNativeWorker != null) {
                mBraveRewardsNativeWorker.AddObserver(this);
                mBraveRewardsNativeWorker.CreateWallet();
            }
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, error.getMessage());
            assert false;
        }

        return START_STICKY;
    }

    //interface BraveRewardsObserver
    @Override
    public void OnWalletInitialized(int error_code){
        if (BraveRewardsNativeWorker.WALLET_CREATED == error_code && OnboardingPrefManager.getInstance().isAdsAvailable()){
            // Enable ads
            BraveAdsNativeHelper.nativeSetAdsEnabled(Profile.getLastUsedProfile());
        }
        else {
            //TODO: handle wallet creation problem
        }
        stopSelf();
    };


    @Override
    public void OnWalletProperties(int error_code){};

    @Override
    public void OnPublisherInfo(int tabId){};

    @Override
    public void OnGetCurrentBalanceReport(String[] report){};

    @Override
    public void OnNotificationAdded(String id, int type, long timestamp, String[] args){};

    @Override
    public void OnNotificationsCount(int count){};

    @Override
    public void OnGetLatestNotification(String id, int type, long timestamp,
                                        String[] args){};

    @Override
    public void OnNotificationDeleted(String id){};

    @Override
    public void OnIsWalletCreated(boolean created){};

    @Override
    public void OnGetPendingContributionsTotal(double amount){};

    @Override
    public void OnGetRewardsMainEnabled(boolean enabled){};

    @Override
    public void OnGetAutoContributeProps(){};

    @Override
    public void OnGetReconcileStamp(long timestamp){};

    @Override
    public void OnRecurringDonationUpdated(){};

    @Override
    public void OnResetTheWholeState(boolean success){};

    @Override
    public void OnRewardsMainEnabled(boolean enabled){};

}