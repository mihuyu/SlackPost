package apps.mihuyu.com.slackpost.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
        builder.setMessage(R.string.dialog_fire_missiles);
        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                mDialog.dismiss();
            }
        });

        List<String> entityList = new ArrayList<>();
        List<String> entityValueList = new ArrayList<>();
//        LinkedHashMap<String, String> channelsMap = new LinkedHashMap<>();
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
//                    channelsMap.put(key, (String) json.get(key));
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }

        }

        final String[] entityValues = entityValueList.toArray( new String[ entityValueList.size() ] );
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
            }

        });
        builder.setView(lv);
        // Create the AlertDialog object and return it
        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(true);

        return mDialog;
    }

    @Override
    public void onPause() {
        super.onPause();
        mDialog.dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContext = null;
        mDialog = null;
    }
}
