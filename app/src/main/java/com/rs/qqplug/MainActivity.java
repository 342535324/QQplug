package com.rs.qqplug;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String UriRoot = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A";
    private static final int OPENIMG_CODE = 1003;//打开图片文件夹
    private static final int BACKUPIMG_CODE = 1004;//备份图片
    private static final int DELETEIMG_CODE = 1005;//删除图片

    private static final int OPENFILE_CODE = 1013;//打开文件缓存文件夹
    private static final int BACKUPFILE_CODE = 1014;//备份文件
    private static final int DELETEFILE_CODE = 1015;//删除文件

    private int saveImgCount = 0;//备份成功的图片数量
    private int saveFileCount = 0;//备份成功的文件数量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.findViewById(R.id.main_btn_0).setOnClickListener(this);
        this.findViewById(R.id.main_btn_1).setOnClickListener(this);
        this.findViewById(R.id.main_btn_2).setOnClickListener(this);

        this.findViewById(R.id.main_btn_3).setOnClickListener(this);
        this.findViewById(R.id.main_btn_4).setOnClickListener(this);
        this.findViewById(R.id.main_btn_5).setOnClickListener(this);

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data.getData() == null) {
            return;
        }
        int current = 1;
        int sum;
        Uri dirUri = null;
        List<DocumentFile> fileList = new ArrayList<>();//图片缓存
        switch (requestCode) {
            case BACKUPIMG_CODE://备份图片
                dirUri = data.getData();
                authUri(dirUri);
                Toast.makeText(getApplicationContext(), "开始备份,请稍后", Toast.LENGTH_SHORT).show();
                saveImgCount = 0;
                //必须再次遍历才能拿到文件夹下的图片文件
                for (DocumentFile children : getChildren(DocumentFile
                        .fromTreeUri(this, dirUri))) {
                    fileList.addAll(java.util.Arrays.asList(children.listFiles()));
                }
                sum = fileList.size();
                for (DocumentFile file : fileList) {
                    backupDir(file, BACKUPIMG_CODE);
                    Toast.makeText(getApplicationContext(), String.format("备份图片成功%d/%d",current++,sum), Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(getApplicationContext(), String.format("成功保存%d张图片至相册的QQMobileChatimg文件夹", saveImgCount), Toast.LENGTH_SHORT).show();
                break;
            case BACKUPFILE_CODE://备份文件
                dirUri = data.getData();
                authUri(dirUri);
                Toast.makeText(getApplicationContext(), "开始备份,请稍后", Toast.LENGTH_SHORT).show();
                saveFileCount = 0;
                //必须再次遍历才能拿到文件夹下的文件
                for (DocumentFile children : getChildren(DocumentFile
                        .fromTreeUri(this, dirUri))) {
                    fileList.addAll(java.util.Arrays.asList(children.listFiles()));
                }
                sum = fileList.size();
                for (DocumentFile file : fileList) {
                    backupDir(file, BACKUPFILE_CODE);
                    Toast.makeText(getApplicationContext(), String.format("备份文件成功%d/%d",current++,sum), Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(getApplicationContext(), String.format("成功保存%d个文件至下载目录的QQMobileFile文件夹", saveFileCount), Toast.LENGTH_SHORT).show();
                break;

            case DELETEIMG_CODE:
                Toast.makeText(getApplicationContext(), "开始删除,请稍后", Toast.LENGTH_SHORT).show();
                dirUri = data.getData();
                authUri(dirUri);
                //必须再次遍历才能拿到文件夹下的图片文件
                fileList =  getChildren(DocumentFile
                        .fromTreeUri(this, dirUri));
                sum = fileList.size();
                for (DocumentFile children :fileList) {
                    deleteDir(children);
                    Toast.makeText(getApplicationContext(), String.format("删除图片成功%d/%d",current++,sum), Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(getApplicationContext(), "删除图片缓存完毕", Toast.LENGTH_SHORT).show();
                break;
            case DELETEFILE_CODE:
                Toast.makeText(getApplicationContext(), "开始删除,请稍后", Toast.LENGTH_SHORT).show();
                dirUri = data.getData();
                authUri(dirUri);
                //必须再次遍历才能拿到文件夹下的图片文件
                fileList =  getChildren(DocumentFile
                        .fromTreeUri(this, dirUri));
                sum = fileList.size();
                for (DocumentFile children : fileList) {
                    deleteDir(children);
                    Toast.makeText(getApplicationContext(), String.format("删除文件成功%d/%d",current++,sum), Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(getApplicationContext(), "删除文件缓存完毕", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * 取得一个被提供授权的可持久化的URI权限
     */
    private void authUri(Uri u) {
        getContentResolver().takePersistableUriPermission(u,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

    }


    /**
     * 复制文件
     *
     * @param src              被复制的文件Uri
     * @param dest             写入Uri
     * @param copyFileRunnable 获取到2个流后的行为
     */
    private boolean copyFile(Uri src, Uri dest, ICopyFileRunnable copyFileRunnable) {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = (FileInputStream) this.getContentResolver().openInputStream(src);
            srcChannel = fis.getChannel();
            if (srcChannel.size() > 0) {
                fos = (FileOutputStream) this.getContentResolver().openOutputStream(dest);
                dstChannel = fos.getChannel();
                copyFileRunnable.run(srcChannel, dstChannel);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("test", "error!!" + e.getMessage());
        } finally {
            try {
                if (srcChannel != null) {
                    srcChannel.close();
                }
                if (dstChannel != null) {
                    dstChannel.close();
                }
                if (fos != null) {
                    fos.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 备份文件
     */
    private void backupDir(DocumentFile file, int model) {
        if (file.isFile()) {
            Uri insertUri = null;
            String fileName = null;
            ContentValues values = new ContentValues();
            ContentResolver resolver = this.getContentResolver();
            switch (model) {
                case BACKUPIMG_CODE:
                    fileName = UUID.randomUUID().toString().replaceAll("-", "") + ".jpg";
                    values.put(MediaStore.Images.Media.DESCRIPTION, "QQMobile chatimg");
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.TITLE, fileName);
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QQMobileChatimg");

                    insertUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    break;
                case BACKUPFILE_CODE:
                    fileName = UUID.randomUUID().toString().replaceAll("-", "");
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.TITLE, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Download/QQMobileFile");

                    insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    break;
            }

            if (insertUri != null && copyFile(file.getUri(), insertUri, (srcChannel, dstChannel) -> {
                try {
                    srcChannel.transferTo(0, srcChannel.size(), dstChannel);
                    Log.i("test", "copy file -->" + srcChannel.size() + " OK!");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("test", "backupDir ERROR" + e.getMessage());
                }
            })) {
                switch (model) {
                    case BACKUPIMG_CODE:
                        saveImgCount++;
                        break;
                    case BACKUPFILE_CODE:
                        saveFileCount++;
                        break;
                }
            }
        } else {
            //如果是文件夹 继续遍历
            for (DocumentFile sub : file.listFiles()) {
                backupDir(sub, model);
            }
        }
    }

    /**
     * 删除文件夹(包含所有子文件)
     *
     * @param file
     */
    private void deleteDir(DocumentFile file) {
        if (!file.isFile()) {
            for (DocumentFile sub : file.listFiles()) {
                deleteDir(sub);
            }
        }
        file.delete();
    }

    /**
     * 获取安全框架直接打开手机QQ聊天缓存的Intent
     *
     * @return Intent
     */
    private Intent getChatimgIntent() {
        Uri uri = Uri.parse(UriRoot + "Android%2Fdata%2Fcom.tencent.mobileqq%2FTencent%2FMobileQQ%2Fchatpic%2Fchatimg");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        //flag看实际业务需要可再补充
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return intent;
    }

    private Intent getChatthumbIntent() {
        Uri uri = Uri.parse(UriRoot + "Android%2Fdata%2Fcom.tencent.mobileqq%2FTencent%2FMobileQQ%2Fchatpic%2Fchatthumb");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        //flag看实际业务需要可再补充
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return intent;
    }


    /**
     * 通过数据库获取指定文件夹下的文件列表
     */
    private List<DocumentFile> getChildren(DocumentFile dir) {
        return java.util.Arrays.asList(dir.listFiles());
    }


    /**
     * 备份QQ图片缓存
     */
    private void backupQQMobileImg() {
        Toast.makeText(getApplicationContext(), "请授权使用存储访问框架", Toast.LENGTH_SHORT).show();
        this.startActivityForResult(getChatimgIntent(), BACKUPIMG_CODE);
    }

    /**
     * 删除QQ图片缓存
     */
    private void deleteQQMobileImg() {
        Toast.makeText(getApplicationContext(), "请授权使用存储访问框架", Toast.LENGTH_SHORT).show();
        this.startActivityForResult(getChatimgIntent(), DELETEIMG_CODE);
    }

    /**
     * 打开QQ图片缓存文件夹
     */
    private void openQQMobileImgDir() {
        Toast.makeText(getApplicationContext(), "请授权使用存储访问框架", Toast.LENGTH_SHORT).show();
        this.startActivityForResult(getChatimgIntent(), OPENIMG_CODE);
    }


    /**
     * 备份QQ文件缓存
     */
    private void backupQQMobileFile() {
        Toast.makeText(getApplicationContext(), "请授权使用存储访问框架", Toast.LENGTH_SHORT).show();
        this.startActivityForResult(getChatthumbIntent(), BACKUPFILE_CODE);
    }

    /**
     * 删除QQ文件缓存
     */
    private void deleteQQMobileFile() {
        Toast.makeText(getApplicationContext(), "请授权使用存储访问框架", Toast.LENGTH_SHORT).show();
        this.startActivityForResult(getChatthumbIntent(), DELETEFILE_CODE);
    }

    /**
     * 打开QQ文件缓存文件夹
     */
    private void openQQMobileFile() {
        Toast.makeText(getApplicationContext(), "请授权使用存储访问框架", Toast.LENGTH_SHORT).show();
        this.startActivityForResult(getChatthumbIntent(), OPENFILE_CODE);
    }


    @SuppressLint("Recycle")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_btn_0:
                backupQQMobileImg();
                break;
            case R.id.main_btn_1:
                if (saveImgCount == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteQQMobileImg();
                        }
                    });
                    builder.setTitle("提示");
                    builder.setMessage("您还没有备份图片,确认要删除所有图片吗?");
                    builder.show();
                } else {
                    deleteQQMobileImg();
                }
                break;
            case R.id.main_btn_2:
                openQQMobileImgDir();
                break;

            case R.id.main_btn_3:
                backupQQMobileFile();
                break;
            case R.id.main_btn_4:
                if (saveFileCount == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteQQMobileFile();
                        }
                    });
                    builder.setTitle("提示");
                    builder.setMessage("您还没有备份文件,确认要删除所有文件吗?");
                    builder.show();
                } else {
                    deleteQQMobileFile();
                }
                break;
            case R.id.main_btn_5:
                openQQMobileFile();
                break;
        }
    }



}
