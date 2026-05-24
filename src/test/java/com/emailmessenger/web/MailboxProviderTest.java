package com.emailmessenger.web;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MailboxProviderTest {

    @Test
    void knownSlugResolvesToProviderWithPreset() {
        Optional<MailboxProvider> resolved = MailboxProvider.fromSlug("gmail");
        assertThat(resolved).isPresent();
        MailboxProvider p = resolved.get();
        assertThat(p.getHost()).isEqualTo("imap.gmail.com");
        assertThat(p.getPort()).isEqualTo(993);
        assertThat(p.isSsl()).isTrue();
        assertThat(p.getAppPasswordUrl()).startsWith("https://");
        assertThat(p.getAppPasswordHelp()).isNotBlank();
    }

    @Test
    void slugMatchIsCaseAndWhitespaceInsensitive() {
        assertThat(MailboxProvider.fromSlug("  GMAIL  ")).contains(MailboxProvider.GMAIL);
        assertThat(MailboxProvider.fromSlug("iCloud")).contains(MailboxProvider.ICLOUD);
    }

    @Test
    void unknownAndNullSlugsReturnEmpty() {
        assertThat(MailboxProvider.fromSlug(null)).isEmpty();
        assertThat(MailboxProvider.fromSlug("")).isEmpty();
        assertThat(MailboxProvider.fromSlug("yahoo")).isEmpty();
    }

    @Test
    void otherProviderHasNoAppPasswordUrlAndBlankHost() {
        MailboxProvider other = MailboxProvider.fromSlug("other").orElseThrow();
        assertThat(other.getAppPasswordUrl()).isNull();
        assertThat(other.getHost()).isEmpty();
        assertThat(other.isSsl()).isTrue();
    }
}
