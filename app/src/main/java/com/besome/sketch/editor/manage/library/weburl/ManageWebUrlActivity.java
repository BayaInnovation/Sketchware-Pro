package com.besome.sketch.editor.manage.library.weburl;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.components.utils.StringsEditorManager;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.UI;

import android.view.Gravity;

public class ManageWebUrlActivity extends BaseAppCompatActivity {

    private ProjectLibraryBean libraryBean;
    private MaterialSwitch libSwitch;
    private TextInputEditText editBaseUrl;
    private LinearLayout llEndpoints;
    private String sc_id;
    private Gson gson = new Gson();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                    
        setContentView(R.layout.manage_library_web_url);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Web URL Manager");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        sc_id = getIntent().getStringExtra("sc_id");
        libraryBean = getIntent().getParcelableExtra("web_url");
        if (libraryBean == null) {
            libraryBean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_WEB_URL);
        }

        libSwitch = findViewById(R.id.lib_switch);
        editBaseUrl = findViewById(R.id.et_base_url);
        llEndpoints = findViewById(R.id.ll_endpoints);

        findViewById(R.id.layout_switch).setOnClickListener(v -> {
            libSwitch.setChecked(!libSwitch.isChecked());
            libraryBean.useYn = libSwitch.isChecked() ? "Y" : "N";
        });
        
        MaterialSwitch switchObfuscate = findViewById(R.id.switch_obfuscate);
        findViewById(R.id.layout_switch_obfuscate).setOnClickListener(v -> {
             switchObfuscate.setChecked(!switchObfuscate.isChecked());
             if (libraryBean.configurations == null) libraryBean.configurations = new HashMap<>();
             libraryBean.configurations.put("obfuscate", switchObfuscate.isChecked());
        });

        findViewById(R.id.btn_add_endpoint).setOnClickListener(v -> addEndpointRow(""));

        loadConfiguration();
    }

    private void loadConfiguration() {
        libSwitch.setChecked("Y".equals(libraryBean.useYn));
        
        MaterialSwitch switchObfuscate = findViewById(R.id.switch_obfuscate);
        if (libraryBean.configurations != null && libraryBean.configurations.containsKey("obfuscate")) {
             Object obf = libraryBean.configurations.get("obfuscate");
             if (obf instanceof Boolean) {
                 switchObfuscate.setChecked((Boolean) obf);
             }
        }
        
        if (libraryBean.data != null && !libraryBean.data.isEmpty()) {
            editBaseUrl.setText(libraryBean.data);
        }
        
        // Load endpoints from configurations map
        // We expect "endpoints" to be a ArrayList<String>
        if (libraryBean.configurations != null && libraryBean.configurations.containsKey("endpoints")) {
            Object endpointsObj = libraryBean.configurations.get("endpoints");
            if (endpointsObj instanceof ArrayList) {
                ArrayList<?> list = (ArrayList<?>) endpointsObj;
                for (Object item : list) {
                    addEndpointRow(String.valueOf(item));
                }
            }
        }
    }

    private void addEndpointRow(String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        TextInputLayout inputLayout = new TextInputLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(0, 8, 8, 8);
        inputLayout.setLayoutParams(params);
        
        TextInputEditText editText = new TextInputEditText(this);
        editText.setText(value);
        editText.setHint("Endpoint (e.g. login)");
        inputLayout.addView(editText);
        
        Button btnDelete = new Button(this);
        btnDelete.setText("Del");
        btnDelete.setOnClickListener(v -> llEndpoints.removeView(row));
        
        row.addView(inputLayout);
        row.addView(btnDelete);
        
        llEndpoints.addView(row);
    }



    private void saveConfiguration() {
        libraryBean.useYn = libSwitch.isChecked() ? "Y" : "N";
        libraryBean.data = editBaseUrl.getText().toString().trim();
        
        ArrayList<String> endpoints = new ArrayList<>();
        for (int i = 0; i < llEndpoints.getChildCount(); i++) {
            View child = llEndpoints.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                if (row.getChildCount() > 0 && row.getChildAt(0) instanceof TextInputLayout) {
                    TextInputLayout til = (TextInputLayout) row.getChildAt(0);
                    if (til.getEditText() != null) {
                        String val = til.getEditText().getText().toString().trim();
                        if (!val.isEmpty()) {
                            endpoints.add(val);
                        }
                    }
                }
            }
        }
        
        if (libraryBean.configurations == null) {
            libraryBean.configurations = new HashMap<>();
        }
        libraryBean.configurations.put("endpoints", endpoints);
        saveToResources();
    }

    private void saveToResources() {
        try {
            String path = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/files/resource/values/strings.xml";
            if (!FileUtil.isExistFile(path)) {
                FileUtil.writeFile(path, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>");
            }
            
            StringsEditorManager sem = new StringsEditorManager();
            sem.sc_id = sc_id;
            ArrayList<HashMap<String, Object>> list = new ArrayList<>();
            
            String content = FileUtil.readFile(path);
            if (content == null || content.isEmpty()) {
                 content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>";
            }
            sem.convertXmlStringsToListMap(content, list);
            
            boolean found = false;
            // Remove existing key if useYn is N, or update if Y
            if ("N".equals(libraryBean.useYn)) {
                for (int i = list.size() - 1; i >= 0; i--) {
                    if ("web_url_base".equals(list.get(i).get("key"))) {
                        list.remove(i);
                        break;
                    }
                }
            } else {
                String dataToSave = libraryBean.data;
                if (libraryBean.configurations != null && 
                    Boolean.TRUE.equals(libraryBean.configurations.get("obfuscate"))) {
                    try {
                        dataToSave = android.util.Base64.encodeToString(
                            libraryBean.data.getBytes(), android.util.Base64.DEFAULT).trim();
                    } catch (Exception e) {}
                }

                for (HashMap<String, Object> map : list) {
                    if ("web_url_base".equals(map.get("key"))) {
                        map.put("text", dataToSave);
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    HashMap<String, Object> newMap = new HashMap<>();
                    newMap.put("key", "web_url_base");
                    newMap.put("text", dataToSave);
                    list.add(newMap);
                }
            }
            
            FileUtil.writeFile(path, sem.convertListMapToXmlStrings(list, sem.notesMap));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        saveConfiguration();
        Intent intent = new Intent();
        intent.putExtra("web_url", libraryBean);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }
}
