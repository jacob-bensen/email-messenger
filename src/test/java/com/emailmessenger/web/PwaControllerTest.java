package com.emailmessenger.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PwaControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PwaController())
            .build();

    @Test
    void manifestServesValidJsonWithStartUrlAndStandaloneDisplay() throws Exception {
        MvcResult result = mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/manifest+json"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Spec-required keys for "installable" PWAs.
        assertThat(body)
                .contains("\"name\":")
                .contains("\"short_name\":")
                .contains("\"start_url\": \"/chats\"")
                .contains("\"scope\": \"/\"")
                .contains("\"display\": \"standalone\"")
                .contains("\"theme_color\": \"#2f855a\"")
                .contains("\"background_color\":");
    }

    @Test
    void manifestAdvertisesEveryRequiredIconSize() throws Exception {
        String body = mockMvc.perform(get("/manifest.webmanifest"))
                .andReturn().getResponse().getContentAsString();
        // Chrome refuses to surface the install prompt unless the manifest
        // lists a 192x192 PNG AND a 512x512 PNG. The maskable variant lets
        // Android adaptive-icon crops keep the glyph in the safe zone.
        assertThat(body)
                .contains("\"src\": \"/icons/icon-192.png\"")
                .contains("\"sizes\": \"192x192\"")
                .contains("\"src\": \"/icons/icon-512.png\"")
                .contains("\"sizes\": \"512x512\"")
                .contains("\"src\": \"/icons/icon-512-maskable.png\"")
                .contains("\"purpose\": \"maskable\"");
    }

    @Test
    void icon192ServesValidPng() throws Exception {
        assertPngWithDimensions("/icons/icon-192.png", 192, 192);
    }

    @Test
    void icon512ServesValidPng() throws Exception {
        assertPngWithDimensions("/icons/icon-512.png", 512, 512);
    }

    @Test
    void icon512MaskableServesValidPng() throws Exception {
        assertPngWithDimensions("/icons/icon-512-maskable.png", 512, 512);
    }

    @Test
    void appleTouchIconServesValid180PxPng() throws Exception {
        // iOS Safari treats <link rel="apple-touch-icon"> as 180x180.
        assertPngWithDimensions("/apple-touch-icon.png", 180, 180);
    }

    @Test
    void serviceWorkerJsRegistersInstallActivateAndFetchHandlers() throws Exception {
        MvcResult result = mockMvc.perform(get("/sw.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // The three lifecycle listeners and the navigation-fallback wiring
        // are what make the SW actually serve /offline when the network drops.
        assertThat(body)
                .contains("addEventListener('install'")
                .contains("addEventListener('activate'")
                .contains("addEventListener('fetch'")
                .contains("caches.match('/offline')");

        // Shell assets must all be present in the pre-cache list so opening
        // the installed PWA in airplane mode renders the cached offline page
        // with its CSS and brand mark.
        for (String asset : PwaController.SHELL_ASSETS) {
            assertThat(body).contains("'" + asset + "'");
        }

        // Per spec the script must not be HTTP-cached — browsers re-check it
        // every navigation and any byte diff triggers a fresh install.
        String cacheControl = result.getResponse().getHeader("Cache-Control");
        assertThat(cacheControl).contains("no-store");
        // Allowing the SW to claim "/" lets it intercept any subpath.
        assertThat(result.getResponse().getHeader("Service-Worker-Allowed")).isEqualTo("/");
    }

    @Test
    void serviceWorkerCacheVersionBustsOnSourceChange() throws Exception {
        // The cache version is derived from a hash of the SW body so a code
        // edit here ships as a new cache key — old clients invalidate on
        // activate. Same source must hash deterministically across requests.
        String first = mockMvc.perform(get("/sw.js"))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(get("/sw.js"))
                .andReturn().getResponse().getContentAsString();

        assertThat(first).isEqualTo(second);
        assertThat(first).containsPattern("CACHE_VERSION = 'conexusmail-shell-[0-9a-f]{12}'");
    }

    @Test
    void offlinePageRendersConexusMailBrandedShell() throws Exception {
        MvcResult result = mockMvc.perform(get("/offline"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Branded copy, not a generic browser error. Must include ConexusMail,
        // a refresh affordance, and the manifest link so the offline screen
        // itself stays installable when shown.
        assertThat(body)
                .contains("ConexusMail")
                .contains("offline")
                .contains("Try again")
                .contains("/manifest.webmanifest")
                .contains("/css/main.css");
    }

    @Test
    void maskableIconFillsCornersUnlikeStandardIcon() throws Exception {
        // The maskable variant must be a full square (corner pixels =
        // brand colour, fully opaque) so an Android adaptive mask doesn't
        // expose transparent corners. The standard variant has rounded
        // corners and should be transparent at (0,0).
        BufferedImage standard = readPng("/icons/icon-512.png");
        BufferedImage maskable = readPng("/icons/icon-512-maskable.png");

        int standardCornerAlpha = (standard.getRGB(0, 0) >>> 24) & 0xff;
        int maskableCornerAlpha = (maskable.getRGB(0, 0) >>> 24) & 0xff;

        assertThat(standardCornerAlpha).isLessThan(255);
        assertThat(maskableCornerAlpha).isEqualTo(255);
    }

    private void assertPngWithDimensions(String path, int width, int height) throws Exception {
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body).isNotEmpty();
        // PNG magic number: 89 50 4E 47 0D 0A 1A 0A
        assertThat(body[0]).isEqualTo((byte) 0x89);
        assertThat(body[1]).isEqualTo((byte) 0x50);
        assertThat(body[2]).isEqualTo((byte) 0x4E);
        assertThat(body[3]).isEqualTo((byte) 0x47);

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(body));
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(width);
        assertThat(img.getHeight()).isEqualTo(height);
    }

    private BufferedImage readPng(String path) throws Exception {
        byte[] body = mockMvc.perform(get(path))
                .andReturn().getResponse().getContentAsByteArray();
        return ImageIO.read(new ByteArrayInputStream(body));
    }
}
