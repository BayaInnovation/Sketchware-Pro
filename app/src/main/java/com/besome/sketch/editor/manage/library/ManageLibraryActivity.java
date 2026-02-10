package com.besome.sketch.editor.manage.library;

import static android.text.TextUtils.isEmpty;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.editor.manage.library.admob.AdmobActivity;
import com.besome.sketch.editor.manage.library.admob.ManageAdmobActivity;
import com.besome.sketch.editor.manage.library.compat.ManageCompatActivity;
import com.besome.sketch.editor.manage.library.firebase.ManageFirebaseActivity;
import com.besome.sketch.editor.manage.library.googlemap.ManageGoogleMapActivity;
import com.besome.sketch.editor.manage.library.material3.Material3LibraryActivity;
import com.besome.sketch.editor.manage.library.material3.Material3LibraryItemView;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import a.a.a.MA;
import a.a.a.jC;
import a.a.a.mB;
import dev.aldi.sayuti.editor.manage.ManageLocalLibraryActivity;
import mod.hey.studios.activity.managers.nativelib.ManageNativelibsActivity;
import mod.hey.studios.util.Helper;
import mod.jbk.editor.manage.library.ExcludeBuiltInLibrariesActivity;
import mod.jbk.editor.manage.library.ExcludeBuiltInLibrariesLibraryItemView;
import pro.sketchware.R;
import pro.sketchware.utility.UI;

import com.besome.sketch.editor.manage.library.weburl.ManageWebUrlActivity;
import com.besome.sketch.editor.manage.library.supabase.ManageSupabaseActivity;
import com.google.gson.Gson;
import pro.sketchware.utility.FileUtil;

public class ManageLibraryActivity extends BaseAppCompatActivity implements View.OnClickListener {

    private final int REQUEST_CODE_ADMOB_ACTIVITY = 234;
    private final int REQUEST_CODE_APPCOMPAT_ACTIVITY = 231;
    private final int REQUEST_CODE_FIREBASE_ACTIVITY = 230;
    private final int REQUEST_CODE_GOOGLE_MAPS_ACTIVITY = 241;
    private final int REQUEST_CODE_MATERIAL3_ACTIVITY = 242;
    private final int REQUEST_CODE_CUSTOM_ITEM_LIBRARY_ACTIVITY = 243;
    private final int REQUEST_CODE_WEB_URL_ACTIVITY = 244;
    private final int REQUEST_CODE_SUPABASE_ACTIVITY = 245;

    private String sc_id;
    private LinearLayout libraryItemLayout;

    private ProjectLibraryBean firebaseLibraryBean;
    private ProjectLibraryBean compatLibraryBean;
    private ProjectLibraryBean admobLibraryBean;
    private ProjectLibraryBean googleMapLibraryBean;
    private ProjectLibraryBean webUrlLibraryBean;
    private ProjectLibraryBean supabaseLibraryBean;

    private String originalFirebaseUseYn = "N";
    private String originalCompatUseYn = "N";
    private String originalAdmobUseYn = "N";
    private String originalGoogleMapUseYn = "N";
    private String originalWebUrlUseYn = "N";
    private String originalSupabaseUseYn = "N";

    private final List<LibraryItemView> libraryItems = new ArrayList<>();

    private LibraryCategoryView addCategoryItem(String text) {
        LibraryCategoryView libraryCategoryView = new LibraryCategoryView(this);
        libraryCategoryView.setTitle(text);
        libraryItemLayout.addView(libraryCategoryView);
        return libraryCategoryView;
    }

    private void addLibraryItem(@Nullable ProjectLibraryBean libraryBean, LibraryCategoryView parent) {
        addLibraryItem(libraryBean, parent, true);
    }

    private void addLibraryItem(@Nullable ProjectLibraryBean libraryBean, LibraryCategoryView parent, boolean addDivider) {
        LibraryItemView libraryItemView;
        libraryItemView = new LibraryItemView(this);
        libraryItemView.setTag(libraryBean != null ? libraryBean.libType : null);
        //noinspection ConstantConditions since the variant if it's nullable handles nulls correctly
        libraryItemView.setData(libraryBean);
        libraryItemView.setOnClickListener(this);

        if (libraryBean.libType == ProjectLibraryBean.PROJECT_LIB_TYPE_LOCAL_LIB || libraryBean.libType == ProjectLibraryBean.PROJECT_LIB_TYPE_NATIVE_LIB) {
            libraryItemView.setHideEnabled();
        }
        parent.addLibraryItem(libraryItemView, addDivider);
        libraryItems.add(libraryItemView);
    }

    private void addCustomLibraryItem(int type, LibraryCategoryView parent) {
        addCustomLibraryItem(type, parent, true);
    }

    private void addCustomLibraryItem(int type, LibraryCategoryView parent, boolean addDivider) {
        LibraryItemView libraryItemView;
        if (type == ProjectLibraryBean.PROJECT_LIB_TYPE_EXCLUDE_BUILTIN_LIBRARIES) {
            libraryItemView = new ExcludeBuiltInLibrariesLibraryItemView(this, sc_id);
            libraryItemView.setData(null);
        } else {
            libraryItemView = new Material3LibraryItemView(this);
            libraryItemView.setData(compatLibraryBean);
        }
        libraryItemView.setTag(type);
        //noinspection ConstantConditions since the variant if it's nullable handles nulls correctly
        libraryItemView.setOnClickListener(this);
        parent.addLibraryItem(libraryItemView, addDivider);
        libraryItems.add(libraryItemView);
    }

    private void toCompatActivity(ProjectLibraryBean compatLibraryBean, ProjectLibraryBean firebaseLibraryBean) {
        Intent intent = new Intent(getApplicationContext(), ManageCompatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("compat", compatLibraryBean);
        intent.putExtra("firebase", firebaseLibraryBean);
        startActivityForResult(intent, REQUEST_CODE_APPCOMPAT_ACTIVITY);
    }

    private void initializeLibrary(@Nullable ProjectLibraryBean libraryBean) {
        if (libraryBean != null) {
            switch (libraryBean.libType) {
                case ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE ->
                        firebaseLibraryBean = libraryBean;
                case ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT -> compatLibraryBean = libraryBean;
                case ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB -> admobLibraryBean = libraryBean;
                case ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP ->
                        googleMapLibraryBean = libraryBean;
                case ProjectLibraryBean.PROJECT_LIB_TYPE_WEB_URL -> webUrlLibraryBean = libraryBean;
                case ProjectLibraryBean.PROJECT_LIB_TYPE_SUPABASE -> supabaseLibraryBean = libraryBean;
            }
        }

        for (LibraryItemView itemView : libraryItems) {
            Object tag = itemView.getTag();
            if (itemView instanceof ExcludeBuiltInLibrariesLibraryItemView) {
                itemView.setData(null);
            } else if (itemView instanceof Material3LibraryItemView) {
                itemView.setData(compatLibraryBean);
            } else if (tag instanceof Integer && libraryBean != null && ((Integer) tag) == libraryBean.libType) {
                itemView.setData(libraryBean);
            }
        }
    }

    private void toAdmobActivity(ProjectLibraryBean libraryBean) {
        Intent intent;
        if (!isEmpty(libraryBean.reserved1) && !isEmpty(libraryBean.appId)) {
            intent = new Intent(getApplicationContext(), ManageAdmobActivity.class);
        } else {
            intent = new Intent(getApplicationContext(), AdmobActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("admob", libraryBean);
        startActivityForResult(intent, REQUEST_CODE_ADMOB_ACTIVITY);
    }

    private void toFirebaseActivity(ProjectLibraryBean libraryBean) {
        Intent intent = new Intent(getApplicationContext(), ManageFirebaseActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("firebase", libraryBean);
        startActivityForResult(intent, REQUEST_CODE_FIREBASE_ACTIVITY);
    }

    private void toGoogleMapActivity(ProjectLibraryBean libraryBean) {
        Intent intent = new Intent(getApplicationContext(), ManageGoogleMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("google_map", libraryBean);
        startActivityForResult(intent, REQUEST_CODE_GOOGLE_MAPS_ACTIVITY);
    }

    private void launchCustomActivity(Class<? extends Activity> toLaunch) {
        Intent intent = new Intent(getApplicationContext(), toLaunch);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("app_compat", compatLibraryBean);
        startActivityForResult(intent, REQUEST_CODE_CUSTOM_ITEM_LIBRARY_ACTIVITY);
    }

    private void toMaterial3Activity() {
        Intent intent = new Intent(getApplicationContext(), Material3LibraryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("compat", compatLibraryBean);
        startActivityForResult(intent, REQUEST_CODE_MATERIAL3_ACTIVITY);
    }

    private void launchActivity(Class<? extends Activity> toLaunch) {
        Intent intent = new Intent(getApplicationContext(), toLaunch);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        startActivity(intent);
    }

    private void saveLibraryConfiguration() {
        jC.c(sc_id).b(compatLibraryBean);
        jC.c(sc_id).c(firebaseLibraryBean);
        jC.c(sc_id).a(admobLibraryBean);
        jC.c(sc_id).d(googleMapLibraryBean);
        jC.c(sc_id).k();
        jC.b(sc_id).a(jC.c(sc_id));
        jC.a(sc_id).a(jC.b(sc_id));
        jC.a(sc_id).a(firebaseLibraryBean);
        jC.a(sc_id).a(admobLibraryBean, jC.b(sc_id));
        jC.a(sc_id).b(googleMapLibraryBean, jC.b(sc_id));
        
        // Save Web URL config manually
        String webUrlPath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/web_url_config";
        FileUtil.writeFile(webUrlPath, new Gson().toJson(webUrlLibraryBean));

        // Save Supabase config manually
        String supabasePath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/supabase_config";
        FileUtil.writeFile(supabasePath, new Gson().toJson(supabaseLibraryBean));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_FIREBASE_ACTIVITY:
                    ProjectLibraryBean libraryBean = data.getParcelableExtra("firebase");
                    initializeLibrary(libraryBean);
                    if (libraryBean.useYn.equals("Y") && !compatLibraryBean.useYn.equals("Y")) {
                        libraryBean = compatLibraryBean;
                        libraryBean.useYn = "Y";
                        initializeLibrary(libraryBean);
                        showFirebaseNeedCompatDialog();
                    }
                    break;

                case REQUEST_CODE_APPCOMPAT_ACTIVITY, REQUEST_CODE_MATERIAL3_ACTIVITY:
                    initializeLibrary(data.getParcelableExtra("compat"));
                    break;

                case REQUEST_CODE_ADMOB_ACTIVITY:
                    initializeLibrary(data.getParcelableExtra("admob"));
                    break;

                case REQUEST_CODE_GOOGLE_MAPS_ACTIVITY:
                    initializeLibrary(data.getParcelableExtra("google_map"));
                    break;
                    
                case REQUEST_CODE_WEB_URL_ACTIVITY:
                    initializeLibrary(data.getParcelableExtra("web_url"));
                    break;
                case REQUEST_CODE_SUPABASE_ACTIVITY:
                    initializeLibrary(data.getParcelableExtra("supabase"));
                    break;

                case REQUEST_CODE_CUSTOM_ITEM_LIBRARY_ACTIVITY:
                    initializeLibrary(null);
                    break;

                default:
            }
        }
    }

    @Override
    public void onBackPressed() {
        k();
        try {
            new Handler().postDelayed(() -> new SaveLibraryTask(this).execute(), 500L);
        } catch (Exception e) {
            e.printStackTrace();
            h();
        }
    }

    @Override
    public void onClick(View v) {
        if (!mB.a()) {
            Object tag = v.getTag();

            if (tag != null) {
                int vTag = (Integer) tag;
                switch (vTag) {
                    case ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE:
                        toFirebaseActivity(firebaseLibraryBean);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT:
                        toCompatActivity(compatLibraryBean, firebaseLibraryBean);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB:
                        toAdmobActivity(admobLibraryBean);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP:
                        toGoogleMapActivity(googleMapLibraryBean);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_LOCAL_LIB:
                        launchActivity(ManageLocalLibraryActivity.class);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_NATIVE_LIB:
                        launchActivity(ManageNativelibsActivity.class);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_EXCLUDE_BUILTIN_LIBRARIES:
                        launchCustomActivity(ExcludeBuiltInLibrariesActivity.class);
                        break;

                    case ProjectLibraryBean.PROJECT_LIB_TYPE_MATERIAL3:
                        toMaterial3Activity();
                        break;
                    case ProjectLibraryBean.PROJECT_LIB_TYPE_WEB_URL:
                        Intent intent = new Intent(getApplicationContext(), ManageWebUrlActivity.class);
                        intent.putExtra("sc_id", sc_id);
                        intent.putExtra("web_url", webUrlLibraryBean);
                        startActivityForResult(intent, REQUEST_CODE_WEB_URL_ACTIVITY);
                        break;
                    case ProjectLibraryBean.PROJECT_LIB_TYPE_SUPABASE:
                        Intent intentSupabase = new Intent(getApplicationContext(), ManageSupabaseActivity.class);
                        intentSupabase.putExtra("sc_id", sc_id);
                        intentSupabase.putExtra("supabase", supabaseLibraryBean);
                        startActivityForResult(intentSupabase, REQUEST_CODE_SUPABASE_ACTIVITY);
                        break;
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        if (!super.isStoragePermissionGranted()) {
            finish();
        }

        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }

        setContentView(R.layout.manage_library);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(Helper.getResString(R.string.design_actionbar_title_library));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        libraryItemLayout = findViewById(R.id.contents);

        ViewCompat.setOnApplyWindowInsetsListener(libraryItemLayout, (v, windowInsets) -> {
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, insets);
            return windowInsets;
        });

        UI.addSystemWindowInsetToPadding(libraryItemLayout, false, false, false, true);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (savedInstanceState == null) {
            compatLibraryBean = jC.c(sc_id).c();
            if (compatLibraryBean == null) {
                compatLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT);
            }
            originalCompatUseYn = compatLibraryBean.useYn;

            firebaseLibraryBean = jC.c(sc_id).d();
            if (firebaseLibraryBean == null) {
                firebaseLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE);
            }
            originalFirebaseUseYn = firebaseLibraryBean.useYn;

            admobLibraryBean = jC.c(sc_id).b();
            if (admobLibraryBean == null) {
                admobLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB);
            }
            originalAdmobUseYn = admobLibraryBean.useYn;

            googleMapLibraryBean = jC.c(sc_id).e();
            if (googleMapLibraryBean == null) {
                googleMapLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP);
            }
            originalGoogleMapUseYn = googleMapLibraryBean.useYn;

            // Load Web URL config
            String webUrlPath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/web_url_config";
            if (FileUtil.isExistFile(webUrlPath)) {
                String json = FileUtil.readFile(webUrlPath);
                webUrlLibraryBean = new Gson().fromJson(json, ProjectLibraryBean.class);
            }
            if (webUrlLibraryBean == null) {
                webUrlLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_WEB_URL);
            }
            originalWebUrlUseYn = webUrlLibraryBean.useYn;

            // Load Supabase config
            String supabasePath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/supabase_config";
            if (FileUtil.isExistFile(supabasePath)) {
                String json = FileUtil.readFile(supabasePath);
                supabaseLibraryBean = new Gson().fromJson(json, ProjectLibraryBean.class);
            }
            if (supabaseLibraryBean == null) {
                supabaseLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_SUPABASE);
            }
            originalSupabaseUseYn = supabaseLibraryBean.useYn;

        } else {
            firebaseLibraryBean = savedInstanceState.getParcelable("firebase");
            originalFirebaseUseYn = savedInstanceState.getString("originalFirebaseUseYn");
            compatLibraryBean = savedInstanceState.getParcelable("compat");
            originalCompatUseYn = savedInstanceState.getString("originalCompatUseYn");
            admobLibraryBean = savedInstanceState.getParcelable("admob");
            originalAdmobUseYn = savedInstanceState.getString("originalAdmobUseYn");
            googleMapLibraryBean = savedInstanceState.getParcelable("google_map");
            originalGoogleMapUseYn = savedInstanceState.getString("originalGoogleMapUseYn");
            webUrlLibraryBean = savedInstanceState.getParcelable("web_url");
            originalWebUrlUseYn = savedInstanceState.getString("originalWebUrlUseYn");
            supabaseLibraryBean = savedInstanceState.getParcelable("supabase");
            originalSupabaseUseYn = savedInstanceState.getString("originalSupabaseUseYn");
        }

        LibraryCategoryView basicCategory = addCategoryItem(null);
        addLibraryItem(compatLibraryBean, basicCategory);
        addCustomLibraryItem(ProjectLibraryBean.PROJECT_LIB_TYPE_MATERIAL3, basicCategory);
        addLibraryItem(firebaseLibraryBean, basicCategory);
        addLibraryItem(admobLibraryBean, basicCategory);
        addLibraryItem(googleMapLibraryBean, basicCategory);
        addLibraryItem(webUrlLibraryBean, basicCategory, false);
        addLibraryItem(supabaseLibraryBean, basicCategory, false);

        LibraryCategoryView advancedCategory = addCategoryItem("Advanced");
        
        // External Libraries (Local Library)
        ProjectLibraryBean localLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_LOCAL_LIB);
        addLibraryItem(localLibraryBean, advancedCategory);
        
        // Native Libraries
        ProjectLibraryBean nativeLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_NATIVE_LIB);
        addLibraryItem(nativeLibraryBean, advancedCategory);
        
        // Exclude Built-in Libraries
        addCustomLibraryItem(ProjectLibraryBean.PROJECT_LIB_TYPE_EXCLUDE_BUILTIN_LIBRARIES, advancedCategory, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!super.isStoragePermissionGranted()) {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        outState.putParcelable("firebase", firebaseLibraryBean);
        outState.putParcelable("compat", compatLibraryBean);
        outState.putParcelable("admob", admobLibraryBean);
        outState.putParcelable("google_map", googleMapLibraryBean);
        outState.putParcelable("web_url", webUrlLibraryBean);
        outState.putString("originalFirebaseUseYn", originalFirebaseUseYn);
        outState.putString("originalCompatUseYn", originalCompatUseYn);
        outState.putString("originalAdmobUseYn", originalAdmobUseYn);
        outState.putString("originalGoogleMapUseYn", originalGoogleMapUseYn);
        outState.putParcelable("web_url", webUrlLibraryBean);
        outState.putString("originalWebUrlUseYn", originalWebUrlUseYn);
        outState.putParcelable("supabase", supabaseLibraryBean);
        outState.putString("originalSupabaseUseYn", originalSupabaseUseYn);
        super.onSaveInstanceState(outState);
    }

    private void showFirebaseNeedCompatDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setIcon(R.drawable.ic_mtrl_firebase);
        dialog.setTitle(Helper.getResString(R.string.common_word_warning));
        dialog.setMessage(Helper.getResString(R.string.design_library_firebase_message_need_compat));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
        dialog.show();
    }

    private static class SaveLibraryTask extends MA {

        private final WeakReference<ManageLibraryActivity> activity;

        public SaveLibraryTask(ManageLibraryActivity activity) {
            super(activity);
            this.activity = new WeakReference<>(activity);
            activity.addTask(this);
        }

        @Override
        public void a() {
            activity.get().h();
            Intent intent = new Intent();
            intent.putExtra("sc_id", activity.get().sc_id);
            intent.putExtra("firebase", activity.get().firebaseLibraryBean);
            intent.putExtra("compat", activity.get().compatLibraryBean);
            intent.putExtra("admob", activity.get().admobLibraryBean);
            intent.putExtra("google_map", activity.get().googleMapLibraryBean);
            activity.get().setResult(RESULT_OK, intent);
            activity.get().finish();
        }

        @Override
        public void a(String idk) {
            activity.get().h();
        }

        @Override
        public void b() {
            try {
                activity.get().saveLibraryConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
