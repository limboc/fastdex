package fastdex.runtime.fastdex;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.InputStream;
import fastdex.common.ShareConstants;
import fastdex.common.utils.FileUtils;
import fastdex.runtime.Constants;
import fastdex.runtime.FastdexRuntimeException;

/**
 * Created by tong on 17/4/29.
 */
public class Fastdex {
    public static final String LOG_TAG = Fastdex.class.getSimpleName();

    private static Fastdex instance;

    final RuntimeMetaInfo runtimeMetaInfo;
    final File patchDirectory;
    final File tempDirectory;
    final File dexDirectory;
    final File resourceDirectory;
    private boolean fastdexEnabled = true;

    public static Fastdex get(Context context) {
        if (instance == null) {
            synchronized (Fastdex.class) {
                if (instance == null) {
                    instance = new Fastdex(context);
                }
            }
        }
        return instance;
    }

    private Context applicationContext;

    public Fastdex(Context applicationContext) {
        this.applicationContext = applicationContext;

        patchDirectory = new File(applicationContext.getFilesDir(), Constants.PATCH_DIR);
        tempDirectory = new File(patchDirectory,Constants.TEMP_DIR);
        dexDirectory = new File(patchDirectory,Constants.DEX_DIR);
        resourceDirectory = new File(patchDirectory,Constants.RES_DIR);

        RuntimeMetaInfo metaInfo = RuntimeMetaInfo.load(this);
        RuntimeMetaInfo assetsMetaInfo = null;
        try {
            InputStream is = applicationContext.getAssets().open(ShareConstants.META_INFO_FILENAME);
            String assetsMetaInfoJson = new String(FileUtils.readStream(is));
            assetsMetaInfo = RuntimeMetaInfo.load(assetsMetaInfoJson);
            if (assetsMetaInfo == null) {
                throw new NullPointerException("AssetsMetaInfo can not be null!!!");
            }
            Log.d(Fastdex.LOG_TAG,"load meta-info from assets: \n" + assetsMetaInfoJson);
            if (metaInfo == null) {
                assetsMetaInfo.save(this);
                metaInfo = assetsMetaInfo;
                File metaInfoFile = new File(patchDirectory, ShareConstants.META_INFO_FILENAME);
                if (!FileUtils.isLegalFile(metaInfoFile)) {
                    throw new FastdexRuntimeException("save meta-info fail: " + metaInfoFile.getAbsolutePath());
                }
            }
            else if (!metaInfo.equals(assetsMetaInfo)) {
                File metaInfoFile = new File(patchDirectory, ShareConstants.META_INFO_FILENAME);
                String metaInfoJson = new String(FileUtils.readContents(metaInfoFile));
                Log.d(Fastdex.LOG_TAG,"load meta-info from files: \n" + metaInfoJson);
                Log.d(Fastdex.LOG_TAG,"meta-info content changed clean");

                FileUtils.cleanDir(patchDirectory);
                FileUtils.cleanDir(tempDirectory);
                assetsMetaInfo.save(this);
                metaInfo = assetsMetaInfo;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            fastdexEnabled = false;
            Log.d(LOG_TAG,"fastdex disabled: " + e.getMessage());
        }

        this.runtimeMetaInfo = metaInfo;
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    public void onAttachBaseContext() {

        //FileUtils.cleanDir(tempDirectory);
    }

    public File getPatchDirectory() {
        return patchDirectory;
    }

    public File getTempDirectory() {
        return tempDirectory;
    }

    public RuntimeMetaInfo getRuntimeMetaInfo() {
        return runtimeMetaInfo;
    }

    public boolean isFastdexEnabled() {
        return fastdexEnabled;
    }
}
