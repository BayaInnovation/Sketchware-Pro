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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        
        // Generate animations based on search query
        executor.execute(() -> {
            try {
                List<LottieAnimation> results = generateAnimationsForQuery(query);
                
                mainHandler.post(() -> {
                    searchResults.clear();
                    searchResults.addAll(results);
                    filterAnimations(selectedCategory);
                    
                    // Show message based on results
                    if (results.isEmpty()) {
                        Toast.makeText(this, "No animations found for '" + query + "'. Try a different search term.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Found " + results.size() + " animations for '" + query + "'", Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                
                mainHandler.post(() -> {
                    searchResults.clear();
                    filterAnimations(selectedCategory);
                    Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Generate animations based on user's search query
     * Directly searches through all available animations using the user's query
     */
    private List<LottieAnimation> generateAnimationsForQuery(String query) {
        List<LottieAnimation> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();
        
        // Get all available animations
        List<IconData> allIcons = getAllAnimations();
        
        // Search through all animations using the user's query directly
        for (IconData icon : allIcons) {
            // Check if query matches the icon name, category, or description
            if (icon.name.toLowerCase().contains(lowerQuery) || 
                icon.category.toLowerCase().contains(lowerQuery) ||
                icon.description.toLowerCase().contains(lowerQuery)) {
                
                results.add(new LottieAnimation(
                    icon.name,
                    icon.category,
                    icon.url,
                    icon.description,
                    icon.loop,
                    false
                ));
            }
        }
        
        // Remove duplicates
        List<LottieAnimation> uniqueResults = new ArrayList<>();
        for (LottieAnimation anim : results) {
            boolean isDuplicate = false;
            for (LottieAnimation existing : uniqueResults) {
                if (existing.getUrl().equals(anim.getUrl())) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                uniqueResults.add(anim);
            }
        }
        
        // If no results found, show helpful message
        if (uniqueResults.isEmpty()) {
            uniqueResults.add(new LottieAnimation(
                "No animations found for '" + query + "'",
                "Info",
                "",
                "Try searching for: loading, success, error, heart, download, settings, user, notification, calendar, etc.",
                false,
                false
            ));
        }
        
        return uniqueResults;
    }
    
    /**
     * Get all available Lordicon animations
     * These are publicly accessible via Lordicon's CDN
     */
    private List<IconData> getAllAnimations() {
        List<IconData> allIcons = new ArrayList<>();
        
        // Loading & Progress animations
        allIcons.add(new IconData("Loading Spinner", "Loading", 
            "https://cdn.lordicon.com/xjovhxra.json", "Circular loading spinner", true));
        allIcons.add(new IconData("Loading Dots", "Loading",
            "https://cdn.lordicon.com/ymrqtsej.json", "Three bouncing dots", true));
        
        // Success & Completion animations
        allIcons.add(new IconData("Success Checkmark", "Success",
            "https://cdn.lordicon.com/lomfljuq.json", "Animated checkmark", false));
        allIcons.add(new IconData("Success Badge", "Success",
            "https://cdn.lordicon.com/yqzmiobz.json", "Success badge animation", false));
        
        // Error & Warning animations
        allIcons.add(new IconData("Error Cross", "Error",
            "https://cdn.lordicon.com/akqsdstj.json", "Error X mark", false));
        allIcons.add(new IconData("Warning Alert", "Warning",
            "https://cdn.lordicon.com/keaiyjcx.json", "Warning triangle", false));
        
        // Search & Find animations
        allIcons.add(new IconData("Search", "Search",
            "https://cdn.lordicon.com/kkvxgpti.json", "Magnifying glass", true));
        
        // Heart & Like animations
        allIcons.add(new IconData("Heart", "Like",
            "https://cdn.lordicon.com/ulnswmkk.json", "Animated heart", false));
        allIcons.add(new IconData("Heart Beat", "Like",
            "https://cdn.lordicon.com/gmzxduhd.json", "Beating heart", true));
        
        // Download animations
        allIcons.add(new IconData("Download", "Download",
            "https://cdn.lordicon.com/wlpxtupd.json", "Download arrow", false));
        
        // Upload animations  
        allIcons.add(new IconData("Upload", "Upload",
            "https://cdn.lordicon.com/wlpxtupd.json", "Upload arrow", false));
        
        // Delete & Remove animations
        allIcons.add(new IconData("Delete", "Delete",
            "https://cdn.lordicon.com/wpyrrmcq.json", "Trash bin", false));
        
        // Settings & Configuration animations
        allIcons.add(new IconData("Settings", "Settings",
            "https://cdn.lordicon.com/lecprnjb.json", "Gear icon", true));
        
        // Notification & Bell animations
        allIcons.add(new IconData("Notification", "Notification",
            "https://cdn.lordicon.com/lznlxwtc.json", "Bell icon", false));
        
        // User & Profile animations
        allIcons.add(new IconData("User", "User",
            "https://cdn.lordicon.com/bhfjfgqz.json", "User profile", false));
        
        // Lock & Security animations
        allIcons.add(new IconData("Lock", "Security",
            "https://cdn.lordicon.com/ktsahwvc.json", "Lock icon", false));
        
        // Home animations
        allIcons.add(new IconData("Home", "Home",
            "https://cdn.lordicon.com/cnbtmfzh.json", "Home icon", false));
        
        // Mail & Message animations
        allIcons.add(new IconData("Email", "Mail",
            "https://cdn.lordicon.com/rhvddzym.json", "Email icon", false));
        
        // Calendar & Time animations
        allIcons.add(new IconData("Calendar", "Calendar",
            "https://cdn.lordicon.com/wmwqvixz.json", "Calendar icon", false));
        
        // Star & Rating animations
        allIcons.add(new IconData("Star", "Rating",
            "https://cdn.lordicon.com/pqxdilfs.json", "Star icon", false));
        
        // Shopping Cart animations
        allIcons.add(new IconData("Shopping Cart", "Shopping",
            "https://cdn.lordicon.com/mqdkoaef.json", "Cart icon", false));
        
        return allIcons;
    }
    
    /**
     * Helper class to store icon data
     */
    private static class IconData {
        String name;
        String category;
        String url;
        String description;
        boolean loop;
        
        IconData(String name, String category, String url, String description, boolean loop) {
            this.name = name;
            this.category = category;
            this.url = url;
            this.description = description;
            this.loop = loop;
        }
    }





    private List<LottieAnimation> parseLottieFilesSearch(String jsonResponse, String query) {
        List<LottieAnimation> results = new ArrayList<>();
        
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            // Check for different possible response formats
            JSONArray dataArray = null;
            
            // Try 'data' field (v2 API format)
            if (jsonObject.has("data")) {
                dataArray = jsonObject.getJSONArray("data");
            }
            // Try 'results' field (alternative format)
            else if (jsonObject.has("results")) {
                dataArray = jsonObject.getJSONArray("results");
            }
            // Try 'animations' field
            else if (jsonObject.has("animations")) {
                dataArray = jsonObject.getJSONArray("animations");
            }
            
            if (dataArray != null && dataArray.length() > 0) {
                for (int i = 0; i < Math.min(dataArray.length(), 20); i++) {
                    JSONObject animObj = dataArray.getJSONObject(i);
                    
                    // Extract title
                    String title = animObj.optString("title", "");
                    if (title.isEmpty()) {
                        title = animObj.optString("name", "Animation " + (i + 1));
                    }
                    
                    // Extract Lottie JSON URL - try multiple field names
                    String lottieUrl = "";
                    
                    // Try direct URL fields
                    lottieUrl = animObj.optString("lottie_url", "");
                    if (lottieUrl.isEmpty()) {
                        lottieUrl = animObj.optString("lottieUrl", "");
                    }
                    if (lottieUrl.isEmpty()) {
                        lottieUrl = animObj.optString("url", "");
                    }
                    if (lottieUrl.isEmpty()) {
                        lottieUrl = animObj.optString("file", "");
                    }
                    if (lottieUrl.isEmpty()) {
                        lottieUrl = animObj.optString("jsonUrl", "");
                    }
                    
                    // Try nested objects
                    if (lottieUrl.isEmpty() && animObj.has("lottieFiles")) {
                        JSONObject lottieFiles = animObj.getJSONObject("lottieFiles");
                        lottieUrl = lottieFiles.optString("url", "");
                    }
                    if (lottieUrl.isEmpty() && animObj.has("file")) {
                        Object fileObj = animObj.get("file");
                        if (fileObj instanceof JSONObject) {
                            lottieUrl = ((JSONObject) fileObj).optString("url", "");
                        }
                    }
                    
                    // Try to construct URL from ID if available
                    if (lottieUrl.isEmpty() && animObj.has("id")) {
                        int id = animObj.optInt("id", 0);
                        if (id > 0) {
                            // Common LottieFiles URL pattern
                            lottieUrl = "https://assets.lottiefiles.com/packages/lf30_" + id + ".json";
                        }
                    }
                    
                    // Get description and creator info
                    String description = animObj.optString("description", "");
                    String creator = "";
                    
                    if (animObj.has("createdBy")) {
                        JSONObject createdBy = animObj.getJSONObject("createdBy");
                        creator = createdBy.optString("name", "");
                        if (creator.isEmpty()) {
                            creator = createdBy.optString("username", "");
                        }
                    } else if (animObj.has("user")) {
                        JSONObject user = animObj.getJSONObject("user");
                        creator = user.optString("name", "");
                        if (creator.isEmpty()) {
                            creator = user.optString("username", "");
                        }
                    } else if (animObj.has("author")) {
                        JSONObject author = animObj.getJSONObject("author");
                        creator = author.optString("name", "");
                    }
                    
                    // Build final description
                    if (description.isEmpty() && !creator.isEmpty()) {
                        description = "By " + creator;
                    } else if (!description.isEmpty() && !creator.isEmpty()) {
                        description = description + " • By " + creator;
                    } else if (description.isEmpty() && creator.isEmpty()) {
                        description = "From LottieFiles";
                    }
                    
                    // Only add if we have a valid URL
                    if (!lottieUrl.isEmpty()) {
                        results.add(new LottieAnimation(
                            title,
                            "Search Result",
                            lottieUrl,
                            description,
                            true,  // Loop by default
                            false  // It's a URL, not an asset
                        ));
                    }
                }
            }
            
        } catch (Exception e) {
            // Parsing failed - return empty results
        }
        
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
                            binding.animationPreview.setRepeatCount(animation.isLoop() ? -1 : 0);
                            binding.animationPreview.playAnimation();
                        } else {
                            // Load from URL with error handling
                            // IMPORTANT: Use setFailureListener to REPLACE the default listener
                            // that logs errors and crashes the app
                            binding.animationPreview.setFailureListener(result -> {
                                // Silently hide preview if animation fails to load (404, network error, etc.)
                                // Don't log or throw - just gracefully hide the broken animation
                                binding.animationPreview.post(() -> {
                                    binding.animationPreview.setVisibility(View.GONE);
                                });
                            });
                            
                            binding.animationPreview.setAnimationFromUrl(url);
                            binding.animationPreview.setRepeatCount(animation.isLoop() ? -1 : 0);
                            binding.animationPreview.playAnimation();
                        }
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
