package com.ga.gestionstock;

import com.ga.gestionstock.service.SmtpMailTransport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpMailTransportTests {
    @Test
    void construitLeMessageEtRespecteLaDesactivation() {
        var sender = mock(JavaMailSender.class);
        var factory = new StaticListableBeanFactory(Map.of("sender", sender));
        var transport = new SmtpMailTransport(factory.getBeanProvider(JavaMailSender.class), "stock@example.test", true);
        transport.envoyer("agent@example.test", "Demande #1", "Votre demande est approuvee.");
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("stock@example.test");
        assertThat(captor.getValue().getTo()).containsExactly("agent@example.test");
        assertThat(captor.getValue().getText()).isEqualTo("Votre demande est approuvee.");
        var disabled = new SmtpMailTransport(factory.getBeanProvider(JavaMailSender.class), "stock@example.test", false);
        assertThatThrownBy(() -> disabled.envoyer("agent@example.test", "Demande #2", "Test")).isInstanceOf(MailSendException.class);
        verifyNoMoreInteractions(sender);
    }
}
