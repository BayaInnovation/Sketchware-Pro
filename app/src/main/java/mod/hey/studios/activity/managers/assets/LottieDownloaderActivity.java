package mod.hey.studios.activity.managers.assets;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.besome.sketch.beans.ProjectFileBean;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mod.hey.studios.util.Helper;

import pro.sketchware.R;
import pro.sketchware.databinding.ActivityLottieDownloaderBinding;
import pro.sketchware.databinding.ItemLottieAnimationBinding;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class LottieDownloaderActivity extends AppCompatActivity {

    private ActivityLottieDownloaderBinding binding;
    private String sc_id;
    private LottieAnimationsAdapter adapter;
    private List<LottieAnimation> allAnimations;
    private List<LottieAnimation> filteredAnimations;
    private List<LottieAnimation> searchResults;
    private String selectedCategory = "All";
    private String currentSearchQuery = "";
    private Runnable searchRunnable;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLottieDownloaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().hasExtra("sc_id")) {
            sc_id = getIntent().getStringExtra("sc_id");
        }

        setupToolbar();
        setupSearchBar();
        setupCategories();
        setupRecyclerView();
        loadAnimations();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        toolbar.setTitle(R.string.lottie_store_title);
        toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
    }

    private void setupSearchBar() {
        TextInputEditText searchInput = binding.searchInput;
        searchResults = new ArrayList<>();
        
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search
                if (searchRunnable != null) {
                    mainHandler.removeCallbacks(searchRunnable);
                }
                
                String query = s.toString().trim();
                
                if (query.isEmpty()) {
                    // Show bundled animations
                    currentSearchQuery = "";
                    filterAnimations(selectedCategory);
                } else {
                    // Debounce search - wait 500ms after user stops typing
                    searchRunnable = () -> performSearch(query);
                    mainHandler.postDelayed(searchRunnable, 500);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategories() {
        ChipGroup chipGroup = binding.categoryChipGroup;
        List<String> categories = LottieAnimationStore.getCategories();

        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            
            if (i == 0) {
                chip.setChecked(true);
            }

            chip.setOnClickListener(v -> {
                selectedCategory = category;
                filterAnimations(category);
            });

            chipGroup.addView(chip);
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.animationsRecycler;
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        
        filteredAnimations = new ArrayList<>();
        adapter = new LottieAnimationsAdapter(filteredAnimations);
        recyclerView.setAdapter(adapter);
    }

    private void loadAnimations() {
        allAnimations = LottieAnimationStore.getCuratedAnimations();
        filterAnimations("All");
    }

    private void filterAnimations(String category) {
        filteredAnimations.clear();
        
        // If searching, show search results instead
        if (!currentSearchQuery.isEmpty()) {
            filteredAnimations.addAll(searchResults);
        } else if (category.equals("All")) {
            filteredAnimations.addAll(allAnimations);
        } else {
            for (LottieAnimation animation : allAnimations) {
                if (animation.getCategory().equals(category)) {
                    filteredAnimations.add(animation);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
    }

    private void performSearch(String query) {
        currentSearchQuery = query;
        
        // Check if query is a URL
        if (query.startsWith("http://") || query.startsWith("https://")) {
            // It's a URL - create a downloadable animation
            searchResults.clear();
            searchResults.add(new LottieAnimation(
                "Custom Animation",
                "Custom",
                query,
                "From URL: " + query,
                true,
                false  // It's a URL, not an asset
            ));
            mainHandler.post(() -> filterAnimations(selectedCategory));
        } else {
            // Search LottieFiles API for suggestions
            executor.execute(() -> {
                try {
                    String encodedQuery = URLEncoder.encode(query, "UTF-8");
                    String apiUrl = "https://lottiefiles.com/api/search?query=" + encodedQuery;
                    
                    String jsonResponse = downloadFromUrl(apiUrl);
                    List<LottieAnimation> results = parseLottieFilesSearch(jsonResponse, query);
                    
                    mainHandler.post(() -> {
                        searchResults.clear();
                        searchResults.addAll(results);
                        filterAnimations(selectedCategory);
                        
                        // Show helpful message if no results
                        if (results.isEmpty()) {
                            Toast.makeText(this, "💡 Tip: Paste a direct Lottie JSON URL to download", Toast.LENGTH_LONG).show();
                        }
                    });
                    
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        // Show empty results on error
                        searchResults.clear();
                        filterAnimations(selectedCategory);
                        Toast.makeText(this, "💡 Tip: Paste a direct Lottie JSON URL to download", Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private List<LottieAnimation> parseLottieFilesSearch(String jsonResponse, String query) {
        List<LottieAnimation> results = new ArrayList<>();
        
        // For now, just return empty list since API only returns search suggestions
        // Users can paste direct URLs to download custom animations
        
        return results;
    }

    private void downloadAnimation(LottieAnimation animation) {
        String filename = animation.getFileName();

        // Check if file already exists
        if (fileExists(filename)) {
            Toast.makeText(this, getString(R.string.lottie_file_exists), Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                String jsonContent;
                
                if (animation.isAsset()) {
                    // Copy from bundled assets
                    jsonContent = loadAssetFile(animation.getUrl());
                } else {
                    // Download from URL
                    jsonContent = downloadFromUrl(animation.getUrl());
                }
                
                if (!isValidLottieJson(jsonContent)) {
                    mainHandler.post(() -> 
                        Toast.makeText(this, R.string.lottie_invalid_json, Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // Save to project assets
                boolean saved = saveToAssets(jsonContent, filename);
                
                mainHandler.post(() -> {
                    if (saved) {
                        Toast.makeText(this, R.string.lottie_save_success, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                    } else {
                        Toast.makeText(this, R.string.lottie_save_error, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> 
                    Toast.makeText(this, R.string.lottie_network_error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private String loadAssetFile(String assetPath) throws Exception {
        InputStream is = getAssets().open(assetPath);
        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = is.read(buffer)) != -1) {
            result.append(new String(buffer, 0, bytesRead));
        }

        is.close();
        return result.toString();
    }

    private String downloadFromUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP error code: " + responseCode);
        }

        InputStream inputStream = connection.getInputStream();
        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            result.append(new String(buffer, 0, bytesRead));
        }

        inputStream.close();
        connection.disconnect();

        return result.toString();
    }

    private boolean fileExists(String filename) {
        FilePathUtil fpu = new FilePathUtil();
        String assetsPath = fpu.getPathAssets(sc_id);
        File file = new File(assetsPath, filename);
        return file.exists();
    }

    private boolean isValidLottieJson(String jsonContent) {
        try {
            JSONObject json = new JSONObject(jsonContent);
            return json.has("v") && json.has("fr") && json.has("ip") && 
                   json.has("op") && json.has("w") && json.has("h");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean saveToAssets(String jsonContent, String filename) {
        try {
            FilePathUtil fpu = new FilePathUtil();
            String assetsPath = fpu.getPathAssets(sc_id);
            
            File assetsDir = new File(assetsPath);
            if (!assetsDir.exists()) {
                assetsDir.mkdirs();
            }

            File outputFile = new File(assetsPath, filename);
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(jsonContent.getBytes());
            fos.close();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // RecyclerView Adapter
    private class LottieAnimationsAdapter extends RecyclerView.Adapter<LottieAnimationsAdapter.ViewHolder> {
        
        private List<LottieAnimation> animations;

        public LottieAnimationsAdapter(List<LottieAnimation> animations) {
            this.animations = animations;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemLottieAnimationBinding binding = ItemLottieAnimationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LottieAnimation animation = animations.get(position);
            holder.bind(animation);
        }

        @Override
        public int getItemCount() {
            return animations.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private ItemLottieAnimationBinding binding;

            public ViewHolder(ItemLottieAnimationBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(LottieAnimation animation) {
                binding.animationName.setText(animation.getName());
                binding.categoryChip.setText(animation.getCategory());
                binding.animationDescription.setText(animation.getDescription());
                
                // Load animation preview only if URL is valid
                String url = animation.getUrl();
                if (url != null && !url.trim().isEmpty()) {
                    try {
                        if (animation.isAsset()) {
                            // Load from bundled assets
                            binding.animationPreview.setAnimation(url);
                        } else {
                            // Load from URL
                            binding.animationPreview.setAnimationFromUrl(url);
                        }
                        binding.animationPreview.setRepeatCount(animation.isLoop() ? -1 : 0);
                        binding.animationPreview.playAnimation();
                    } catch (Exception e) {
                        // Fallback if loading fails - hide preview
                        binding.animationPreview.setVisibility(View.GONE);
                    }
                } else {
                    // No valid URL - hide preview for info items
                    binding.animationPreview.setVisibility(View.GONE);
                }

                // Hide download button for non-downloadable items (like info tips)
                if (animation.isDownloadable()) {
                    binding.downloadButton.setVisibility(View.VISIBLE);
                    binding.downloadButton.setOnClickListener(v -> {
                        downloadAnimation(animation);
                    });
                } else {
                    binding.downloadButton.setVisibility(View.GONE);
                }
            }
        }
    }
}
