/*
 * Part of this code come from frameworks/base/cmds/pm
 */

package com.genymobile.cfdroid;

import android.content.Context;
import android.util.Log;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.backup.BackupManager;
import android.content.res.Configuration;
import com.android.internal.app.LocalePicker;
import java.util.Locale;

import java.util.TimeZone;
import android.app.IAlarmManager;
import android.os.ServiceManager;

import com.android.internal.view.IInputMethodManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import java.util.List;


public final class CfDroid {
    private static String TAG = "CfDroid";
    private String[] mArgs;
    private int mNextArg;

    public static void main(String[] args) {
        new CfDroid().run(args);
    }

    public void run(String[] args) {
	if (args.length < 1) {
	    showUsage();
	    System.exit(-1);
	}

        mArgs = args;
        String op = args[0];
        mNextArg = 1;
	if ("language".equals(op)) {
	    int ret = runLanguage();
	    if (ret != 0)
		System.exit(-1);
	} else if ("timezone".equals(op)) {
	    int ret = runTimezone();
	    if (ret != 0)
		System.exit(-1);
	} else if ("layout".equals(op)) {
	    int ret = runLayout();
	    if (ret != 0)
		System.exit(-1);
	} else {
            System.err.println("Error: didn't specify any command");
	    showUsage();
	    System.exit(-1);
	}
    }

    /**
     * Language sub-command
     * @return: 0 on success
     */
    private int runLanguage() {
        String action = nextArg();
        if (action == null) {
            System.err.println("Error: didn't specify action");
            showUsage();
	    return 1;
        }

	if ("get".equals(action)) {
	    try {
		IActivityManager am = ActivityManagerNative.getDefault();
		Configuration config = am.getConfiguration();
		String language = config.locale.getLanguage();
		System.out.println(language);
	    } catch(android.os.RemoteException e) {
		Log.e(TAG, "android.os.RemoteException");
		System.out.println("unknown");
		return 1;
	    } catch(Exception e) {
		Log.e(TAG, "Exception");
		System.out.println("unknown");
		return 1;
	    }
	} else if("set".equals(action)) {
	    String strLocale = nextArg();
	    if(strLocale != null) {
		Locale locale = new Locale(strLocale);
		// code from com.android.internal.app.LocalePicker
		try {
		    IActivityManager am = ActivityManagerNative.getDefault();
		    Configuration config = am.getConfiguration();

		    config.locale = locale;

		    // indicate this isn't some passing default - the user wants this remembered
		    config.userSetLocale = true;

		    am.updateConfiguration(config);
		    // Trigger the dirty bit for the Settings Provider.
		    BackupManager.dataChanged("com.android.providers.settings");
		} catch (android.os.RemoteException e) {
		    Log.e(TAG, "error while setting locale");
		    return 1;
		}
		// /com.android.internal.app.LocalePicker
	    } else {
		showUsage();
		return 1;
	    }
	} else {
            System.err.println("Error: unknown action '" + action + "'");
            showUsage();
	    return 1;
	}
	return 0;
    }

    /**
     * Timezone sub-command
     * @return: 0 on success
     */
    private int runTimezone() {
        String action = nextArg();
        if (action == null) {
            System.err.println("Error: didn't specify action");
            showUsage();
	    return 1;
        }

	if ("get".equals(action)) {
	    System.out.println(TimeZone.getDefault().getID());
	} else if ("set".equals(action)) {
	    String strTz = nextArg();
	    if( strTz != null ) {
		IAlarmManager alarmManager = 
		    IAlarmManager.Stub.asInterface(ServiceManager.getService(Context.ALARM_SERVICE));
		if (alarmManager == null ) {
		    Log.e(TAG, "Cannot found alarm manager");
		    return 1;
		}
		try {
		    alarmManager.setTimeZone(strTz);
		} catch(android.os.RemoteException e) {
		    Log.e(TAG, "error setting timezone");
		    return 1;
		}
	    } else {
		showUsage();
		return 1;
	    }
	} else {
            System.err.println("Error: unknown action '" + action + "'");
            showUsage();
	    return 1;
	}
	return 0;
    }

    /**
     * Timezone sub-command
     * @return: 0 on success
     * fr
     * fr@qwertz
     * en_US
     */
    private int runLayout() {
        String action = nextArg();
        if (action == null) {
            System.err.println("Error: didn't specify action");
            showUsage();
	    return 1;
        }


	IInputMethodManager imm = 
	    IInputMethodManager.Stub.asInterface(ServiceManager.getService(Context.INPUT_METHOD_SERVICE));
	if (imm == null ) {
	    Log.e(TAG, "Cannot found inputmethod manager");
	    return 1;
	}
	if ("get".equals(action)) {
	    try {
		InputMethodSubtype ims = imm.getCurrentInputMethodSubtype();
		String locale = ims.getLocale();
		String extra = ims.getExtraValue();
		String locale_extra = keyboardLayoutFromExtra(extra);

		if(locale_extra != null)
		    System.out.println(locale+"@"+locale_extra);
		else
		    System.out.println(locale);
	    } catch(android.os.RemoteException e) {
		Log.e(TAG, "remote exception");
		return 1;
	    }
	} else if("set".equals(action)) {
	    /*
	     * TODO: We can switch to a keyboard that is not in the registered
	     * input methods in settings.
	     * We need to manage this list.
	     */
	    String strLayout = nextArg();
	    String[] strLayoutArray = strLayout.split("@");
	    String layout = strLayoutArray[0];
	    String layoutExtra = null;

	    if(strLayoutArray.length > 1) {
		layoutExtra = strLayoutArray[1];
	    }

	    if( strLayout != null ) {
		try {
		    //find the layout we want because I do not want to build one
		    List<InputMethodInfo> inputMethodProperties = 
			imm.getEnabledInputMethodList();
		    //FIXME: we take the first one, need to test with a physical keyboard
		    InputMethodInfo imi = inputMethodProperties.get(0);
		    InputMethodSubtype ims = null;
		    for(int j=0; j < imi.getSubtypeCount(); ++j) {
			InputMethodSubtype tmp = imi.getSubtypeAt(j);
			String tmpLayoutExtra = keyboardLayoutFromExtra(tmp.getExtraValue());
			if(layoutExtra == null && tmpLayoutExtra != null ||
			   layoutExtra != null && tmpLayoutExtra == null)
			    continue;

			if(tmp.getLocale().equals(layout) &&
			   ( layoutExtra == tmpLayoutExtra ||
			     layoutExtra.equals(tmpLayoutExtra)) ) {

			    ims = tmp;
			    break;
			}
		    }
		    if (ims != null) {
			imm.setInputMethodAndSubtype(null, imi.getId(), ims);
		    } else {
			Log.e(TAG, "Cannot find layout");
			return 1;
		    }
		}catch(android.os.RemoteException e) {
		    Log.e(TAG, "remote exception");
		    return 1;
		}
	    }
	}
	return 0;
    }


    /**
     * KeyboardLayoutSet=qwertz,AsciiCapable,isAdditionalSubtype  => qwertz
     * TrySuppressingImeSwitcher,AsciiCapable,SupportTouchPositionCorrection => null
     */
    private String keyboardLayoutFromExtra(String extra) {
	String locale_extra = null;

	String[] extraArray = extra.split(",");
	for(int i = 0; i < extraArray.length; ++i) {
	    String extraElm = extraArray[i];
	    if(extraElm.startsWith("KeyboardLayoutSet=")) {
		locale_extra = extraElm.split("=")[1];
	    }
	}
	return locale_extra;
    }

    private String nextArg() {
	if (mNextArg >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mNextArg];
        mNextArg++;
        return arg;
    }


    private static void showUsage() {
	System.err.println("usage: cf-droid language [get|set value]");
	System.err.println("       cf-droid timezone [get|set value]");
	System.err.println("       cf-droid layout [get|set value]");
    }
}
