package com.besome.sketch.editor.manage.library.supabase;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.utility.SketchwareUtil;

public class ManageSupabaseActivity extends BaseAppCompatActivity implements View.OnClickListener {

    private MaterialSwitch libSwitch;
    private EditText edUrl;
    private EditText edAnonKey;
    private EditText edServiceKey;
    private ProjectLibraryBean supabaseLibraryBean;
    private String sc_id;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_library_manage_supabase);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        supabaseLibraryBean = getIntent().getParcelableExtra("supabase");
        sc_id = getIntent().getStringExtra("sc_id");

        libSwitch = findViewById(R.id.lib_switch);
        edUrl = findViewById(R.id.ed_url);
        edAnonKey = findViewById(R.id.ed_anon_key);
        edServiceKey = findViewById(R.id.ed_service_key);

        findViewById(R.id.layout_switch).setOnClickListener(this);
        ((TextView) findViewById(R.id.tv_enable)).setText(Helper.getResString(R.string.design_library_supabase_title_enable));

        Button btnConsole = findViewById(R.id.btn_console);
        btnConsole.setOnClickListener(this);
        
        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(this);

        configure();
    }

    private void configure() {
        if (supabaseLibraryBean != null) {
            libSwitch.setChecked("Y".equals(supabaseLibraryBean.useYn));
            edUrl.setText(supabaseLibraryBean.data);
            edAnonKey.setText(supabaseLibraryBean.reserved1);
            edServiceKey.setText(supabaseLibraryBean.reserved2);
        }
    }

    private void save() {
        if (supabaseLibraryBean == null) {
            supabaseLibraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_SUPABASE);
        }

        supabaseLibraryBean.useYn = libSwitch.isChecked() ? "Y" : "N";
        supabaseLibraryBean.data = edUrl.getText().toString().trim();
        supabaseLibraryBean.reserved1 = edAnonKey.getText().toString().trim();
        supabaseLibraryBean.reserved2 = edServiceKey.getText().toString().trim();
    }

    @Override
    public void onBackPressed() {
        save();
        Intent intent = new Intent();
        intent.putExtra("supabase", supabaseLibraryBean);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.layout_switch) {
            if (libSwitch.isChecked()) {
                // If turning off
                showDisableDialog();
            } else {
                libSwitch.setChecked(true);
            }
        } else if (id == R.id.btn_console) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://supabase.com/dashboard"));
                startActivity(intent);
            } catch (Exception e) {
                SketchwareUtil.toast("Could not open browser");
            }
        } else if (id == R.id.btn_save) {
            onBackPressed();
        }
    }

    private void showDisableDialog() {
         MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_word_warning));
        dialog.setMessage("Are you sure you want to disable Supabase? Note: This will not remove components automatically.");
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_disable), (v, which) -> {
            libSwitch.setChecked(false);
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }
}
