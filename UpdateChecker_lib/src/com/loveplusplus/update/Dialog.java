package com.loveplusplus.update;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.yyxu.download.services.DownloadService;
import com.yyxu.download.utils.MyIntents;

import eu.inmite.android.lib.dialogs.SimpleDialogFragment;

/**
 * Extends SimpleDialogFragment class of StyledDialogs library.
 *
 * @see <a href="https://github.com/inmite/android-styled-dialogs">inmite - Android Styled Dialogs</a> Required.
 * @see SimpleDialogFragment class to extend.
 */
public class Dialog extends SimpleDialogFragment {

	

    @Override
    public Builder build(Builder builder) {
        Context context = getActivity().getApplicationContext();
       
        builder.setTitle(context.getString(R.string.newUpdateAvailable));
        builder.setMessage(getArguments().getString(Constants.APK_UPDATE_CONTENT));
        
        builder.setPositiveButton(context.getString(R.string.dialogPositiveButton), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	goToDownload();
                dismiss();
            }
        });
        builder.setNegativeButton(context.getString(R.string.dialogNegativeButton), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return builder;
    }

    /**
     * 运行下载服务
     */
    private void goToDownload() {
    	Intent intent=new Intent(getActivity().getApplicationContext(),DownloadService.class);
    	intent.putExtra(MyIntents.URL, getArguments().getString(Constants.APK_DOWNLOAD_URL));
    	intent.putExtra(MyIntents.TYPE, MyIntents.Types.ADD);
    	getActivity().startService(intent);
    }
}
