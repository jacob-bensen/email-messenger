package com.emailmessenger.web;

/**
 * View model for the landing-hero demo embed. Built from
 * {@link LandingProperties.Video} in {@code MarketingController}; null in
 * the model when no provider/id is configured (template falls back to the
 * static chat-bubble mock).
 *
 * <p>Embed URLs use privacy-enhanced hosts where available
 * ({@code youtube-nocookie.com}) so the cookie-consent banner stays
 * honest: no third-party cookies are set until the visitor clicks play.
 */
public record LandingVideo(String provider, String embedUrl, String posterUrl, String title) {

    private static final java.util.regex.Pattern SAFE_ID =
            java.util.regex.Pattern.compile("[A-Za-z0-9_-]{4,64}");

    static LandingVideo from(LandingProperties.Video video) {
        if (video == null) {
            return null;
        }
        String provider = video.getProvider();
        String id = video.getId();
        if (provider.isEmpty() || id.isEmpty() || !SAFE_ID.matcher(id).matches()) {
            return null;
        }
        String embedUrl = switch (provider) {
            case "youtube" -> "https://www.youtube-nocookie.com/embed/" + id
                    + "?rel=0&modestbranding=1&autoplay=1";
            case "loom" -> "https://www.loom.com/embed/" + id + "?autoplay=1";
            default -> null;
        };
        if (embedUrl == null) {
            return null;
        }
        return new LandingVideo(provider, embedUrl, video.getPosterUrl(), video.getTitle());
    }
}
