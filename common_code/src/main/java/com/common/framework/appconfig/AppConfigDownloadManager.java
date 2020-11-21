/**
 *
 */
package com.common.framework.appconfig;

import android.app.Activity;
import android.os.Message;

import com.common.framework.Threadpool.ThreadPoolTool;
import com.common.framework.application.CrashHandler;
import com.common.framework.download.DownLoadFileBean;
import com.common.framework.download.DownLoadFileManager;
import com.common.framework.http.HttpRequest;
import com.common.framework.interfaces.HandlerListener;
import com.common.framework.utils.ShowLog;
import com.common.framework.utils.ZzLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * ****************************************************************************************************************************************************************************
 * app 配置下载管理器，
 *
 * @author :Atar
 * @createTime:2017-9-22上午10:08:39
 * @version:1.0.0
 * @modifyTime:
 * @modifyAuthor:
 * @description:如下载 配置动态皮肤，配置动态文件 根据版本号下载,并且根据配置最低多少版本才下载 等
 * ****************************************************************************************************************************************************************************
 */
public class AppConfigDownloadManager {

    private static String TAG = AppConfigDownloadManager.class.getSimpleName();

    private static AppConfigDownloadManager instance;
    public static String defaultVersion = "1.0.00";

    public synchronized static AppConfigDownloadManager getInstance() {
        if (instance == null) {
            instance = new AppConfigDownloadManager();
        }
        return instance;
    }

    /**
     * 配置文件按照版本下载
     *
     * @param activity
     * @param handlerListener
     * @param newVersion          配置文件版本
     * @param replaceMinVersion   配置文件在 >= 多少版本 开始下载替换
     * @param which
     * @param fileUrl
     * @param fileThreadNum
     * @param deleteOnExit
     * @param strDownloadFileName
     * @param strDownloadDir
     * @author :Atar
     * @createTime:2017-9-22下午2:36:45
     * @version:1.0.0
     * @modifyTime:
     * @modifyAuthor:
     * @description:
     */
    public void downLoadAppConfigFile(Activity activity, final HandlerListener handlerListener, final String newVersion, String replaceMinVersion, final int which, final String fileUrl,
                                      final int fileThreadNum, final boolean deleteOnExit, final String strDownloadFileName, final String strDownloadDir) {
        try {
            if (newVersion == null || replaceMinVersion == null || newVersion.length() == 0 || replaceMinVersion.length() == 0) {
                return;
            }
            File downloadFile = new File(strDownloadDir + strDownloadFileName);
            if (downloadFile.exists()) {// 存在下载文件
                String localVersion = AppConfigModel.getInstance().getString(fileUrl, defaultVersion);
                ZzLog.e("fileUrl 本地版本:" + localVersion);
                if (newVersion.compareToIgnoreCase(localVersion) > 0
                        && localVersion.compareToIgnoreCase(replaceMinVersion) >= 0) {
                    // 配置新版本比本地版本大 同时 允许替换的版本比本地版本大

                } else {
                    ShowLog.i(TAG, fileUrl + ":不下载替换");
                    return;
                }
            }
            ZzLog.e("fileUrl:" + fileUrl + ":不存在");
            DownLoadFileManager.getInstance().downLoad(activity, new HandlerListener() {
                @Override
                public void onHandlerData(Message msg) {
                    switch (msg.what) {
                        case DownLoadFileBean.DOWLOAD_FLAG_FAIL:
                            ShowLog.i(TAG, fileUrl + ":下载失败");
                            break;
                        case DownLoadFileBean.DOWLOAD_FLAG_SUCCESS:
                            AppConfigModel.getInstance().putString(fileUrl, newVersion, true);
                            ShowLog.i(TAG, fileUrl + ":下载成功");
                            break;
                    }
                    if (handlerListener != null) {
                        handlerListener.onHandlerData(msg);
                    }
                }
            }, which, fileUrl, fileThreadNum, deleteOnExit, strDownloadFileName, strDownloadDir);
        } catch (Exception e) {
            ShowLog.e(TAG, CrashHandler.crashToString(e));
        }
    }

    /**
     * 读取服务端配置son
     *
     * @param FileUrl
     * @param mHttpCallBackResult
     * @author :Atar
     * @createTime:2017-9-22下午3:08:01
     * @version:1.0.0
     * @modifyTime:
     * @modifyAuthor:
     * @description:
     */
    public void getServerJson(final String FileUrl, final HttpCallBackResult mHttpCallBackResult) {
        ThreadPoolTool.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                String result = "";
                HttpURLConnection httpConnection = null;
                try {
                    URL url = new URL(FileUrl);
                    httpConnection = HttpRequest.getHttpURLConnection(url, 5000);
                    HttpRequest.setConHead(httpConnection);
                    httpConnection.connect();
                    int responseCode = httpConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                        InputStream instream = httpConnection.getInputStream();
                        if (instream != null) {
                            InputStreamReader inputreader = new InputStreamReader(instream);
                            BufferedReader buffreader = new BufferedReader(inputreader);
                            String line;
                            // 分行读取
                            while ((line = buffreader.readLine()) != null) {
                                ZzLog.i(line);
                                result += line;
                            }
                            inputreader.close();
                            instream.close();
                            buffreader.close();
                        }
                    }
                } catch (Exception e) {
                    ShowLog.w(TAG, CrashHandler.crashToString(e));
                } finally {
                    if (httpConnection != null)
                        httpConnection.disconnect();// 关闭连接
                }
                if (mHttpCallBackResult != null) {
                    mHttpCallBackResult.onResult(result);
                }
            }
        });
    }

    /**
     * 读取服务端json成功回调
     *
     * @author :Atar
     * @createTime:2017-9-22下午3:05:54
     * @version:1.0.0
     * @modifyTime:
     * @modifyAuthor:
     * @description:
     */
    public interface HttpCallBackResult {
        /**
         * 返回数据
         *
         * @param result
         * @author :Atar
         * @createTime:2017-9-22下午3:06:30
         * @version:1.0.0
         * @modifyTime:
         * @modifyAuthor:
         * @description:
         */
        void onResult(String result);
    }
}
