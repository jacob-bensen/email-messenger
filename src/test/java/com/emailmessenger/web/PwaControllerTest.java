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
                .contains("\"start_url\": \"/threads\"")
                .contains("\"scope\": \"/\"")
                .contains("\"display\": \"standalone\"")
                .contains("\"theme_color\": \"#4f80ff\"")
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
