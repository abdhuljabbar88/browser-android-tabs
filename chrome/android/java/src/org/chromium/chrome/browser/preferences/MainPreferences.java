// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.preferences;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;

import org.chromium.base.ContextUtils;
import org.chromium.base.VisibleForTesting;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.ChromeFeatureList;
import org.chromium.chrome.browser.net.spdyproxy.DataReductionProxySettings;
import org.chromium.chrome.browser.night_mode.NightModeUtils;
import org.chromium.chrome.browser.partnercustomizations.HomepageManager;
import org.chromium.chrome.browser.password_manager.ManagePasswordsReferrer;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.preferences.autofill_assistant.AutofillAssistantPreferences;
import org.chromium.chrome.browser.preferences.datareduction.DataReductionPreferenceFragment;
import org.chromium.chrome.browser.preferences.developer.DeveloperPreferences;
import org.chromium.chrome.browser.search_engines.TemplateUrl;
import org.chromium.chrome.browser.search_engines.TemplateUrlService;
import org.chromium.chrome.browser.signin.SigninManager;
import org.chromium.chrome.browser.sync.ProfileSyncService;
import org.chromium.chrome.browser.util.FeatureUtilities;
import org.chromium.chrome.browser.onboarding.OnboardingPrefManager;
import org.chromium.chrome.browser.ChromeFeatureList;
import org.chromium.chrome.browser.BraveRewardsHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * The main settings screen, shown when the user first opens Settings.
 */
public class MainPreferences extends PreferenceFragment
        implements TemplateUrlService.LoadListener, ProfileSyncService.SyncStateChangedListener,
                   SigninManager.SignInStateObserver {
    // public static final String PREF_ACCOUNT_SECTION = "account_section";
    // public static final String PREF_SIGN_IN = "sign_in";
    // public static final String PREF_SYNC_AND_SERVICES = "sync_and_services";
    public static final String PREF_STANDARD_SEARCH_ENGINE = "standard_search_engine";
    public static final String PREF_PRIVATE_SEARCH_ENGINE = "private_search_engine";
    public static final String PREF_SAVED_PASSWORDS = "saved_passwords";
    public static final String PREF_HOMEPAGE = "homepage";
    public static final String PREF_CLOSING_TABS = "closing_tabs";
    public static final String PREF_UI_THEME = "ui_theme";
    //public static final String PREF_DATA_REDUCTION = "data_reduction";
    public static final String PREF_NOTIFICATIONS = "notifications";
    public static final String PREF_WELCOME_TOUR = "welcome_tour";
    public static final String PREF_LANGUAGES = "languages";
    public static final String PREF_DOWNLOADS = "downloads";
    public static final String PREF_DEVELOPER = "developer";
    public static final String PREF_AUTOFILL_ASSISTANT = "autofill_assistant";
    public static final String PREF_BRAVE_REWARDS = "brave_rewards";

    public static final String AUTOFILL_GUID = "guid";
    // Needs to be in sync with kSettingsOrigin[] in
    // chrome/browser/ui/webui/options/autofill_options_handler.cc
    public static final String SETTINGS_ORIGIN = "Chrome settings";

    private final ManagedPreferenceDelegate mManagedPreferenceDelegate;
    private final Map<String, Preference> mAllPreferences = new HashMap<>();
    //private SignInPreference mSignInPreference;

    public MainPreferences() {
        setHasOptionsMenu(true);
        mManagedPreferenceDelegate = createManagedPreferenceDelegate();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPreferences();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mSignInPreference.onPreferenceFragmentDestroyed();
    }

    @Override
    public void onStart() {
        super.onStart();
        /*if (SigninManager.get().isSigninSupported()) {
            SigninManager.get().addSignInStateObserver(this);
            mSignInPreference.registerForUpdates();
        }
        ProfileSyncService syncService = ProfileSyncService.get();
        if (syncService != null) {
            syncService.addSyncStateChangedListener(this);
        }*/
    }

    @Override
    public void onStop() {
        super.onStop();
        /*if (SigninManager.get().isSigninSupported()) {
            SigninManager.get().removeSignInStateObserver(this);
            mSignInPreference.unregisterForUpdates();
        }
        ProfileSyncService syncService = ProfileSyncService.get();
        if (syncService != null) {
            syncService.removeSyncStateChangedListener(this);
        }*/
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    private void createPreferences() {
        PreferenceUtils.addPreferencesFromResource(this, R.xml.main_preferences);
        cachePreferences();

        // if (ChromeFeatureList.isEnabled(ChromeFeatureList.UNIFIED_CONSENT)) {
        //     mSignInPreference.setOnStateChangedCallback(this::onSignInPreferenceStateChanged);
        // } else {
        //     getPreferenceScreen().removePreference(findPreference(PREF_ACCOUNT_SECTION));
        //     getPreferenceScreen().removePreference(findPreference(PREF_SYNC_AND_SERVICES));
        // }

        updatePasswordsPreference();
        setManagedPreferenceDelegateForPreference(PREF_STANDARD_SEARCH_ENGINE);
        setManagedPreferenceDelegateForPreference(PREF_PRIVATE_SEARCH_ENGINE);
        //setManagedPreferenceDelegateForPreference(PREF_DATA_REDUCTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // If we are on Android O+ the Notifications preference should lead to the Android
            // Settings notifications page, not to Chrome's notifications settings page.
            Preference notifications = findPreference(PREF_NOTIFICATIONS);
            notifications.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE,
                        ContextUtils.getApplicationContext().getPackageName());
                startActivity(intent);
                // We handle the click so the default action (opening NotificationsPreference)
                // isn't triggered.
                return true;
            });
        } else {
            // Since the Content Suggestions Notification feature has been removed, the
            // Notifications Preferences page only contains a link to per-website notification
            // settings, which can be access through Site Settings, so don't show the Notifications
            // Preferences page.

            // TODO(crbug.com/944912): Have the Offline Pages Prefetch Notifier start using the pref
            // that can be set on this page, then re-enable.
            getPreferenceScreen().removePreference(findPreference(PREF_NOTIFICATIONS));
        }

        if (!TemplateUrlService.getInstance().isLoaded()) {
            TemplateUrlService.getInstance().registerLoadListener(this);
            TemplateUrlService.getInstance().load();
        }

        // This checks whether the flag for Downloads Preferences is enabled.
        if (!ChromeFeatureList.isEnabled(ChromeFeatureList.DOWNLOADS_LOCATION_CHANGE)) {
            getPreferenceScreen().removePreference(findPreference(PREF_DOWNLOADS));
        }

        // This checks whether Autofill Assistant is enabled and was shown at least once (only then
        // will the AA switch be assigned a value).
        /*if (!ChromeFeatureList.isEnabled(ChromeFeatureList.AUTOFILL_ASSISTANT)
                || !ContextUtils.getAppSharedPreferences().contains(
                        AutofillAssistantPreferences.PREF_AUTOFILL_ASSISTANT_SWITCH)) {
            getPreferenceScreen().removePreference(findPreference(PREF_AUTOFILL_ASSISTANT));
        }*/

        Preference welcomeTour = findPreference(PREF_WELCOME_TOUR);
        welcomeTour.setOnPreferenceClickListener(preference -> {

            final TextView titleTextView = new TextView (getActivity());
            titleTextView.setText(getActivity().getResources().getString(R.string.welcome_tour_dialog_text));
            int padding = BraveRewardsHelper.dp2px(20);
            titleTextView.setPadding(padding,padding,padding,padding);
            titleTextView.setTextSize(18); 
            titleTextView.setTextColor(getActivity().getResources().getColor(R.color.standard_mode_tint));
            titleTextView.setTypeface(null, Typeface.BOLD);

            AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.Theme_Chromium_AlertDialog)
            .setView(titleTextView)
            .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    OnboardingPrefManager.getInstance().showOnboarding(getActivity(), true);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
            alertDialog.show();
            return true;
        });

        if (!ChromeFeatureList.isEnabled(ChromeFeatureList.BRAVE_REWARDS)) {
            getPreferenceScreen().removePreference(welcomeTour);
        }
    }

    /**
     * Stores all preferences in memory so that, if they needed to be added/removed from the
     * PreferenceScreen, there would be no need to reload them from 'main_preferences.xml'.
     */
    private void cachePreferences() {
        int preferenceCount = getPreferenceScreen().getPreferenceCount();
        for (int index = 0; index < preferenceCount; index++) {
            Preference preference = getPreferenceScreen().getPreference(index);
            mAllPreferences.put(preference.getKey(), preference);
        }
        //mSignInPreference = (SignInPreference) mAllPreferences.get(PREF_SIGN_IN);
    }

    private void setManagedPreferenceDelegateForPreference(String key) {
        ChromeBasePreference chromeBasePreference = (ChromeBasePreference) mAllPreferences.get(key);
        chromeBasePreference.setManagedPreferenceDelegate(mManagedPreferenceDelegate);
    }

    private void updatePreferences() {
        /*if (SigninManager.get().isSigninSupported()) {
            addPreferenceIfAbsent(PREF_SIGN_IN);
        } else {
            removePreferenceIfPresent(PREF_SIGN_IN);
        }

        updateSyncAndServicesPreference();*/
        updateSearchEnginePreference(PREF_STANDARD_SEARCH_ENGINE);
        updateSearchEnginePreference(PREF_PRIVATE_SEARCH_ENGINE);

        if (HomepageManager.shouldShowHomepageSetting()) {
            Preference homepagePref = addPreferenceIfAbsent(PREF_HOMEPAGE);
            if (FeatureUtilities.isNewTabPageButtonEnabled()) {
                homepagePref.setTitle(R.string.options_startup_page_title);
            }
            setOnOffSummary(homepagePref, HomepageManager.getInstance().getPrefHomepageEnabled());
        } else {
            removePreferenceIfPresent(PREF_HOMEPAGE);
        }

        Preference closingTabsPref = addPreferenceIfAbsent(PREF_CLOSING_TABS);
        setOnOffSummary(closingTabsPref, ClosingTabsManager.isClosingAllTabsClosesBraveEnabled());

        // if (NightModeUtils.isNightModeSupported() && FeatureUtilities.isNightModeAvailable()) {
        //     addPreferenceIfAbsent(PREF_UI_THEME);
        // } else {
        //     removePreferenceIfPresent(PREF_UI_THEME);
        // }

        if (DeveloperPreferences.shouldShowDeveloperPreferences()) {
            addPreferenceIfAbsent(PREF_DEVELOPER);
        } else {
            removePreferenceIfPresent(PREF_DEVELOPER);
        }

        if (!ChromeFeatureList.isEnabled(ChromeFeatureList.BRAVE_REWARDS) ||
            PrefServiceBridge.getInstance().isSafetynetCheckFailed()) {
            removePreferenceIfPresent(PREF_BRAVE_REWARDS);
        }

        /*ChromeBasePreference dataReduction =
                (ChromeBasePreference) findPreference(PREF_DATA_REDUCTION);
        dataReduction.setSummary(DataReductionPreferenceFragment.generateSummary(getResources()));*/
    }

    private Preference addPreferenceIfAbsent(String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        if (preference == null) getPreferenceScreen().addPreference(mAllPreferences.get(key));
        return mAllPreferences.get(key);
    }

    private void removePreferenceIfPresent(String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        if (preference != null) getPreferenceScreen().removePreference(preference);
    }

    /*private void updateSyncAndServicesPreference() {
        if (!ChromeFeatureList.isEnabled(ChromeFeatureList.UNIFIED_CONSENT)) return;

        ChromeBasePreference syncAndServices =
                (ChromeBasePreference) findPreference(PREF_SYNC_AND_SERVICES);
        syncAndServices.setIcon(SyncPreferenceUtils.getSyncStatusIcon(getActivity()));
        syncAndServices.setSummary(SyncPreferenceUtils.getSyncStatusSummary(getActivity()));
    }*/

    private void updateSearchEnginePreference(String prefSearchName) {
        if (!TemplateUrlService.getInstance().isLoaded()) {
            ChromeBasePreference searchEnginePref =
                    (ChromeBasePreference) findPreference(prefSearchName);
            searchEnginePref.setEnabled(false);
            return;
        }

        String defaultSearchEngineName = TemplateUrlService.getInstance().getDefaultSearchEngineName(prefSearchName.equals(PREF_PRIVATE_SEARCH_ENGINE));
        Preference searchEnginePreference = findPreference(prefSearchName);
        searchEnginePreference.setEnabled(true);
        searchEnginePreference.setSummary(defaultSearchEngineName);
    }

    private void updatePasswordsPreference() {
        Preference passwordsPreference = findPreference(PREF_SAVED_PASSWORDS);
        passwordsPreference.setOnPreferenceClickListener(preference -> {
            PreferencesLauncher.showPasswordSettings(
                    getActivity(), ManagePasswordsReferrer.CHROME_SETTINGS);
            return true;
        });
    }

    private void setOnOffSummary(Preference pref, boolean isOn) {
        pref.setSummary(getResources().getString(isOn ? R.string.text_on : R.string.text_off));
    }

    // SigninManager.SignInStateObserver implementation.
    @Override
    public void onSignedIn() {
        // After signing in or out of a managed account, preferences may change or become enabled
        // or disabled.
        new Handler().post(() -> updatePreferences());
    }

    @Override
    public void onSignedOut() {
        updatePreferences();
    }

    private void onSignInPreferenceStateChanged() {
        // Remove "Account" section header if the personalized sign-in promo is shown.
        /*if (mSignInPreference.getState() == SignInPreference.State.PERSONALIZED_PROMO) {
            removePreferenceIfPresent(PREF_ACCOUNT_SECTION);
        } else {
            addPreferenceIfAbsent(PREF_ACCOUNT_SECTION);
        }*/
    }

    // TemplateUrlService.LoadListener implementation.
    @Override
    public void onTemplateUrlServiceLoaded() {
        TemplateUrlService.getInstance().unregisterLoadListener(this);
        updateSearchEnginePreference(PREF_STANDARD_SEARCH_ENGINE);
        updateSearchEnginePreference(PREF_PRIVATE_SEARCH_ENGINE);
    }

    @Override
    public void syncStateChanged() {
        //updateSyncAndServicesPreference();
    }

    @VisibleForTesting
    ManagedPreferenceDelegate getManagedPreferenceDelegateForTest() {
        return mManagedPreferenceDelegate;
    }

    private ManagedPreferenceDelegate createManagedPreferenceDelegate() {
        return new ManagedPreferenceDelegate() {
            @Override
            public boolean isPreferenceControlledByPolicy(Preference preference) {
                /*if (PREF_DATA_REDUCTION.equals(preference.getKey())) {
                    return DataReductionProxySettings.getInstance().isDataReductionProxyManaged();
                }
                if (PREF_STANDARD_SEARCH_ENGINE.equals(preference.getKey())) {
                    return TemplateUrlService.getInstance().isDefaultSearchManaged();
                }*/
                return false;
            }

            @Override
            public boolean isPreferenceClickDisabledByPolicy(Preference preference) {
                /*if (PREF_DATA_REDUCTION.equals(preference.getKey())) {
                    DataReductionProxySettings settings = DataReductionProxySettings.getInstance();
                    return settings.isDataReductionProxyManaged()
                            && !settings.isDataReductionProxyEnabled();
                }
                if (PREF_STANDARD_SEARCH_ENGINE.equals(preference.getKey())) {
                    return TemplateUrlService.getInstance().isDefaultSearchManaged();
                }*/
                return isPreferenceControlledByPolicy(preference)
                        || isPreferenceControlledByCustodian(preference);
            }
        };
    }
}
