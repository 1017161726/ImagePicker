package com.lzy.imagepicker.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import com.lzy.imagepicker.DataHolder;
import com.lzy.imagepicker.ImageDataSource;
import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.R;
import com.lzy.imagepicker.adapter.ImageFolderAdapter;
import com.lzy.imagepicker.adapter.ImageRecyclerAdapter;
import com.lzy.imagepicker.adapter.ImageRecyclerAdapter.OnImageItemClickListener;
import com.lzy.imagepicker.bean.ImageFolder;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.util.Utils;
import com.lzy.imagepicker.view.FolderPopUpWindow;
import com.lzy.imagepicker.view.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * ================================================
 * ��    �ߣ�jeasonlzy������Ң Github��ַ��https://github.com/jeasonlzy0216
 * ��    ����1.0
 * �������ڣ�2016/5/19
 * ��    ����
 * �޶���ʷ��
 * 2017-03-17
 *
 * @author nanchen
 * ������ֱ�Ӵ����Ƿ�ü��������Լ�ֱ������
 * ================================================
 */
public class ImageGridActivity extends ImageBaseActivity implements ImageDataSource.OnImagesLoadedListener, OnImageItemClickListener, ImagePicker.OnImageSelectedListener, View.OnClickListener {

    public static final int REQUEST_PERMISSION_STORAGE = 0x01;
    public static final int REQUEST_PERMISSION_CAMERA = 0x02;
    public static final String EXTRAS_TAKE_PICKERS = "TAKE";
    public static final String EXTRAS_IMAGES = "IMAGES";

    private ImagePicker imagePicker;

    private boolean isOrigin = false;  //�Ƿ�ѡ��ԭͼ
    private View mFooterBar;     //�ײ���
    private Button mBtnOk;       //ȷ����ť
    private View mllDir; //�ļ����л���ť
    private TextView mtvDir; //��ʾ��ǰ�ļ���
    private TextView mBtnPre;      //Ԥ����ť
    private ImageFolderAdapter mImageFolderAdapter;    //ͼƬ�ļ��е�������
    private FolderPopUpWindow mFolderPopupWindow;  //ImageSet��PopupWindow
    private List<ImageFolder> mImageFolders;   //���е�ͼƬ�ļ���
    //    private ImageGridAdapter mImageGridAdapter;  //ͼƬ�Ź���չʾ��������
    private boolean directPhoto = false; // Ĭ�ϲ���ֱ�ӵ�ȡ���
    private RecyclerView mRecyclerView;
    private ImageRecyclerAdapter mRecyclerAdapter;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        directPhoto = savedInstanceState.getBoolean(EXTRAS_TAKE_PICKERS, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRAS_TAKE_PICKERS, directPhoto);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_grid);

        imagePicker = ImagePicker.getInstance();
        imagePicker.clear();
        imagePicker.addOnImageSelectedListener(this);
        if (imagePicker.getSelectLimit() == 0 || imagePicker.getSelectLimit() == 1) {
            imagePicker.setSelectLimit(1);
            imagePicker.setMultiMode(false);
        }

        Intent data = getIntent();
        // ������ֱ������
        if (data != null && data.getExtras() != null) {
            directPhoto = data.getBooleanExtra(EXTRAS_TAKE_PICKERS, false); // Ĭ�ϲ���ֱ�Ӵ����
            if (directPhoto) {
                checkToCapture();
            }
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(EXTRAS_IMAGES);
            imagePicker.setSelectedImages(images);
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);

        findViewById(R.id.btn_back).setOnClickListener(this);
        mBtnOk = (Button) findViewById(R.id.btn_ok);
        mBtnOk.setOnClickListener(this);
        mBtnPre = (TextView) findViewById(R.id.btn_preview);
        mBtnPre.setOnClickListener(this);
        mFooterBar = findViewById(R.id.footer_bar);
        mllDir = findViewById(R.id.ll_dir);
        mllDir.setOnClickListener(this);
        mtvDir = (TextView) findViewById(R.id.tv_dir);
        if (imagePicker.isMultiMode()) {
            mBtnOk.setVisibility(View.VISIBLE);
            mBtnPre.setVisibility(View.VISIBLE);
        } else {
            mBtnOk.setVisibility(View.GONE);
            mBtnPre.setVisibility(View.GONE);
        }

//        mImageGridAdapter = new ImageGridAdapter(this, null);
        mImageFolderAdapter = new ImageFolderAdapter(this, null);
        mRecyclerAdapter = new ImageRecyclerAdapter(this, null);

        onImageSelected(0, null, false);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new ImageDataSource(this, null, this);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_STORAGE);
            }
        } else {
            new ImageDataSource(this, null, this);
        }

    }

    private void checkToCapture() {
        if (!(checkPermission(Manifest.permission.CAMERA)) || !checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, ImageGridActivity.REQUEST_PERMISSION_CAMERA);
        } else {
            imagePicker.takePicture(this, ImagePicker.REQUEST_CODE_TAKE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new ImageDataSource(this, null, this);
            } else {
                showToast(getString(R.string.ip_str_no_permission));
            }
        } else if (requestCode == REQUEST_PERMISSION_CAMERA) {
            boolean denied = false;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    denied = true;
                }
            }
            if (!denied) {
                imagePicker.takePicture(this, ImagePicker.REQUEST_CODE_TAKE);
            } else {
                showToast(getString(R.string.ip_str_no_camera_permission));
            }
        }
    }

    @Override
    protected void onDestroy() {
        imagePicker.removeOnImageSelectedListener(this);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_ok) {
            Intent intent = new Intent();
            intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, imagePicker.getSelectedImages());
            setResult(ImagePicker.RESULT_CODE_ITEMS, intent);  //��ѡ������ü��ü�����������
            finish();
        } else if (id == R.id.ll_dir) {
            if (mImageFolders == null) {
                Log.i("ImageGridActivity", "�����ֻ�û��ͼƬ");
                return;
            }
            //����ļ��а�ť
            createPopupFolderList();
            mImageFolderAdapter.refreshData(mImageFolders);  //ˢ������
            if (mFolderPopupWindow.isShowing()) {
                mFolderPopupWindow.dismiss();
            } else {
                mFolderPopupWindow.showAtLocation(mFooterBar, Gravity.NO_GRAVITY, 0, 0);
                //Ĭ��ѡ��ǰѡ�����һ������Ŀ¼�ܶ�ʱ��ֱ�Ӷ�λ����ѡ�е���Ŀ
                int index = mImageFolderAdapter.getSelectIndex();
                index = index == 0 ? index : index - 1;
                mFolderPopupWindow.setSelection(index);
            }
        } else if (id == R.id.btn_preview) {
            Intent intent = new Intent(ImageGridActivity.this, ImagePreviewActivity.class);
            intent.putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, 0);
            intent.putExtra(ImagePicker.EXTRA_IMAGE_ITEMS, imagePicker.getSelectedImages());
            intent.putExtra(ImagePreviewActivity.ISORIGIN, isOrigin);
            intent.putExtra(ImagePicker.EXTRA_FROM_ITEMS, true);
            startActivityForResult(intent, ImagePicker.REQUEST_CODE_PREVIEW);
        } else if (id == R.id.btn_back) {
            //������ذ�ť
            finish();
        }
    }

    /**
     * ����������ListView
     */
    private void createPopupFolderList() {
        mFolderPopupWindow = new FolderPopUpWindow(this, mImageFolderAdapter);
        mFolderPopupWindow.setOnItemClickListener(new FolderPopUpWindow.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                mImageFolderAdapter.setSelectIndex(position);
                imagePicker.setCurrentImageFolderPosition(position);
                mFolderPopupWindow.dismiss();
                ImageFolder imageFolder = (ImageFolder) adapterView.getAdapter().getItem(position);
                if (null != imageFolder) {
//                    mImageGridAdapter.refreshData(imageFolder.images);
                    mRecyclerAdapter.refreshData(imageFolder.images);
                    mtvDir.setText(imageFolder.name);
                }
            }
        });
        mFolderPopupWindow.setMargin(mFooterBar.getHeight());
    }

    @Override
    public void onImagesLoaded(List<ImageFolder> imageFolders) {
        this.mImageFolders = imageFolders;
        imagePicker.setImageFolders(imageFolders);
        if (imageFolders.size() == 0) {
//            mImageGridAdapter.refreshData(null);
            mRecyclerAdapter.refreshData(null);
        } else {
//            mImageGridAdapter.refreshData(imageFolders.get(0).images);
            mRecyclerAdapter.refreshData(imageFolders.get(0).images);
        }
//        mImageGridAdapter.setOnImageItemClickListener(this);
        mRecyclerAdapter.setOnImageItemClickListener(this);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        if (mRecyclerView.getItemDecorationCount() < 1) {
            mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(3, Utils.dp2px(this, 2), false));
        }
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mImageFolderAdapter.refreshData(imageFolders);
    }

    @Override
    public void onImageItemClick(View view, ImageItem imageItem, int position) {
        //�����Ƿ��������ťȷ��λ��
        position = imagePicker.isShowCamera() ? position - 1 : position;
        if (imagePicker.isMultiMode()) {
            Intent intent = new Intent(ImageGridActivity.this, ImagePreviewActivity.class);
            intent.putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, position);

            /**
             * 2017-03-20
             *
             * ��Ȼ���������ý��н�������õ���������ʽ����
             */

            // ��˵�����ᵼ�´���ͼƬ��ʱ�����
//            intent.putExtra(ImagePicker.EXTRA_IMAGE_ITEMS, imagePicker.getCurrentImageFolderItems());

            // �����������ûᵼ��Ԥ��������ֱ�ӷ��ؿ�ָ��
            DataHolder.getInstance().save(DataHolder.DH_CURRENT_IMAGE_FOLDER_ITEMS, imagePicker.getCurrentImageFolderItems());
            intent.putExtra(ImagePreviewActivity.ISORIGIN, isOrigin);
            startActivityForResult(intent, ImagePicker.REQUEST_CODE_PREVIEW);  //����Ƕ�ѡ�����ͼƬ����Ԥ������
        } else {
            imagePicker.clearSelectedImages();
            imagePicker.addSelectedImageItem(position, imagePicker.getCurrentImageFolderItems().get(position), true);
            if (imagePicker.isFreeCrop) {
                Intent intent = new Intent(ImageGridActivity.this, FreeCropActivity.class);
                startActivityForResult(intent, ImagePicker.REQUEST_CODE_CROP);  //�������ɲü�����
            } else if (imagePicker.isCrop()) {
                Intent intent = new Intent(ImageGridActivity.this, ImageCropActivity.class);
                startActivityForResult(intent, ImagePicker.REQUEST_CODE_CROP);  //�����ϰ汾�ü�����
            } else {
                Intent intent = new Intent();
                intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, imagePicker.getSelectedImages());
                setResult(ImagePicker.RESULT_CODE_ITEMS, intent);   //��ѡ����Ҫ�ü�����������
                finish();
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    @Override
    public void onImageSelected(int position, ImageItem item, boolean isAdd) {
        if (imagePicker.getSelectImageCount() > 0) {
            mBtnOk.setText(getString(R.string.ip_select_complete, imagePicker.getSelectImageCount(), imagePicker.getSelectLimit()));
            mBtnOk.setEnabled(true);
            mBtnPre.setEnabled(true);
            mBtnPre.setText(getResources().getString(R.string.ip_preview_count, imagePicker.getSelectImageCount()));
            mBtnPre.setTextColor(ContextCompat.getColor(this, R.color.ip_text_primary_inverted));
            mBtnOk.setTextColor(ContextCompat.getColor(this, R.color.ip_text_primary_inverted));
        } else {
            mBtnOk.setText(getString(R.string.ip_complete));
            mBtnOk.setEnabled(false);
            mBtnPre.setEnabled(false);
            mBtnPre.setText(getResources().getString(R.string.ip_preview));
            mBtnPre.setTextColor(ContextCompat.getColor(this, R.color.ip_text_secondary_inverted));
            mBtnOk.setTextColor(ContextCompat.getColor(this, R.color.ip_text_secondary_inverted));
        }
//        mImageGridAdapter.notifyDataSetChanged();
//        mRecyclerAdapter.notifyItemChanged(position); // 17/4/21 fix the position while click img to preview
//        mRecyclerAdapter.notifyItemChanged(position + (imagePicker.isShowCamera() ? 1 : 0));// 17/4/24  fix the position while click right bottom preview button
        for (int i = imagePicker.isShowCamera() ? 1 : 0; i < mRecyclerAdapter.getItemCount(); i++) {
            if (mRecyclerAdapter.getItem(i).path != null && mRecyclerAdapter.getItem(i).path.equals(item.path)) {
                mRecyclerAdapter.notifyItemChanged(i);
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getExtras() != null) {
            if (resultCode == ImagePicker.RESULT_CODE_BACK) {
                isOrigin = data.getBooleanExtra(ImagePreviewActivity.ISORIGIN, false);
            } else {
                //�����ս��淵��
                //��� X , û��ѡ����Ƭ
                if (data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS) == null) {
                    //ʲô������ ֱ�ӵ������
                } else {
                    //˵���ǴӲü�ҳ����������ݣ�ֱ�ӷ��ؾͿ���
                    setResult(ImagePicker.RESULT_CODE_ITEMS, data);
                }
                finish();
            }
        } else {
            //��������գ���Ϊ�ü�ָ���˴洢��Uri�����Է��ص�dataһ��Ϊnull
            if (resultCode == RESULT_OK && requestCode == ImagePicker.REQUEST_CODE_TAKE) {
                //���͹㲥֪ͨͼƬ������
                ImagePicker.galleryAddPic(this, imagePicker.getTakeImageFile());

                /**
                 * 2017-03-21 �Ի�������ת����
                 */
                String path = imagePicker.getTakeImageFile().getAbsolutePath();

                //��������������������ѡ�б���
                ImageItem imageItem = new ImageItem();
                imageItem.path = path;
                imagePicker.addSelectedImageItem(0, imageItem, true);
                //�Ƿ���Ҫ�ü�����ѡģʽ��֧�ֲü�
                if (!imagePicker.isMultiMode()) {
                    if (imagePicker.isFreeCrop) {
                        Intent intent = new Intent(ImageGridActivity.this, FreeCropActivity.class);
                        startActivityForResult(intent, ImagePicker.REQUEST_CODE_CROP);  //�������ɲü�����
                        return;
                    } else if (imagePicker.isCrop()) {
                        Intent intent = new Intent(ImageGridActivity.this, ImageCropActivity.class);
                        startActivityForResult(intent, ImagePicker.REQUEST_CODE_CROP);  //�����ϰ汾�ü�����
                        return;
                    }
                }

                Intent intent = new Intent();
                intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, imagePicker.getSelectedImages());
                setResult(ImagePicker.RESULT_CODE_ITEMS, intent);   //��ѡ����Ҫ�ü�����������
                finish();
            } else if (directPhoto) {
                finish();
            }
        }
    }

}