package mod.hey.studios.activity.managers.assets;

public class LottieAnimation {
    private String name;
    private String category;
    private String url;
    private String description;
    private boolean loop;
    private boolean isAsset; // true if loading from assets, false if from URL

    public LottieAnimation(String name, String category, String url, String description, boolean loop, boolean isAsset) {
        this.name = name;
        this.category = category;
        this.url = url;
        this.description = description;
        this.loop = loop;
        this.isAsset = isAsset;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean isAsset() {
        return isAsset;
    }

    public String getFileName() {
        if (isAsset) {
            // For assets, return the asset path as filename
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return name.toLowerCase().replace(" ", "_") + ".json";
    }

    public boolean isDownloadable() {
        // Animation is downloadable if it has a valid URL
        return url != null && !url.trim().isEmpty();
    }
}
