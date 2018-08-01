package apps.mihuyu.com.slackpost.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import apps.mihuyu.com.slackpost.ChooseTransparentActivity;
import apps.mihuyu.com.slackpost.R;
import apps.mihuyu.com.slackpost.SettingsActivity;

public class FireMissilesDialogFragment extends DialogFragment {

    private static Dialog mDialog = null;
    private Context mContext;

    public static FireMissilesDialogFragment newInstance(Bundle bundle) {
        FireMissilesDialogFragment instance = new FireMissilesDialogFragment();
        instance.setArguments(bundle);
        return instance;
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mDialog != null) {
            return mDialog;
        }
        // Use the Builder class for convenient dialog construction
        if (mContext == null) {
            mContext = getActivity();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                mDialog.dismiss();
                ((ChooseTransparentActivity)mContext).finishActivity();
            }
        });

        List<String> entityList = new ArrayList<>();
        List<String> entityValueList = new ArrayList<>();
        Map<String, ?> map = mContext.getSharedPreferences(CommonConst.KEY_CHANNEL_LIST, Context.MODE_PRIVATE).getAll();
        if (map != null && map.size() > 0) {
            try {
                String obj = (String) map.get(CommonConst.KEY_CHANNEL_LIST);
                JSONObject json = new JSONObject(obj);
                Iterator<String> iterator = json.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    entityList.add((String) json.get(key));
                    entityValueList.add(key);
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }

        final String[] entityValues = entityValueList.toArray( new String[ entityValueList.size() ] );

        // Channel一覧設定
        if (entityValues.length != 0) {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, entityList);
            ListView lv = new ListView(mContext);
            lv.setAdapter(arrayAdapter);
            lv.setScrollingCacheEnabled(true);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int which, long l) {
                    String selected = entityValues[which];
                    ((ChooseTransparentActivity)mContext).onPost(selected);
                    mDialog.dismiss();
                    ((ChooseTransparentActivity)mContext).finishActivity();
                }

            });
            builder.setView(lv);
            builder.setMessage(R.string.dialog_fire_missiles);
        } else {
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    mDialog.dismiss();
                    ((ChooseTransparentActivity)mContext).finishActivity();
                    Intent intent = new Intent();
                    intent.setClassName(mContext, SettingsActivity.class.getName());
                    startActivity(intent);
                }
            });
            builder.setMessage(R.string.dialog_no_channel_list);
        }

        // Create the AlertDialog object and return it
        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_UP &&
                        keyCode == KeyEvent.KEYCODE_BACK) {
                    mDialog.dismiss();
                    ((ChooseTransparentActivity)mContext).finishActivity();
                    return false;
                }
                return false;
            }
        });

        return mDialog;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContext = null;
        mDialog = null;
    }
}
