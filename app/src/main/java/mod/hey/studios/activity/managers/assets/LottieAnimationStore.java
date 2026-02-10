package mod.hey.studios.activity.managers.assets;

import java.util.ArrayList;
import java.util.List;

public class LottieAnimationStore {
    
    public static List<LottieAnimation> getCuratedAnimations() {
        List<LottieAnimation> animations = new ArrayList<>();
        
        // Loading Animations (from bundled assets)
        animations.add(new LottieAnimation(
            "Loading Dots",
            "Loading",
            "lottie/loading_dots.json",
            "Simple loading dots animation",
            true,
            true  // isAsset = true
        ));
        
        // Success Animations (from bundled assets)
        animations.add(new LottieAnimation(
            "Success Checkmark",
            "Success",
            "lottie/success_checkmark.json",
            "Animated success checkmark",
            false,
            true  // isAsset = true
        ));
        
        // Error Animations (from bundled assets)
        animations.add(new LottieAnimation(
            "Error Cross",
            "Error",
            "lottie/error_cross.json",
            "Error cross mark",
            false,
            true  // isAsset = true
        ));
        
        return animations;
    }
    
    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("All");
        categories.add("Loading");
        categories.add("Success");
        categories.add("Error");
        return categories;
    }
}
