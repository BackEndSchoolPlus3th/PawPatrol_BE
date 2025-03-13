package com.patrol.domain.member.auth.service;

import com.patrol.api.member.auth.dto.EmailDto;
import com.patrol.global.exceptions.ErrorCodes;
import com.patrol.global.exceptions.ServiceException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender emailSender;
  private final StringRedisTemplate redisTemplate;
  private final SecureRandom secureRandom = new SecureRandom();


  private static final String KEY_PREFIX = "email:verification:";

  @Async
  public void sendVerificationEmail(String email) {
    String verificationCode = _generateVerificationCode();
    _saveVerificationCode(email, verificationCode);
    
    String title = "이메일 인증 코드";
    String text = String.format("""
        안녕하세요. 퍼피구조대 입니다.
        인증 코드: %s
        
        해당 인증 코드는 30분간 유효합니다.
        """, verificationCode);

    EmailDto emailDto = EmailDto.builder()
            .receiver(email)
            .title(title)
            .text(text)
            .build();

    _sendEmail(emailDto);
  }

  public boolean verifyCode(String email, String code) {
    String key = KEY_PREFIX + email;
    String savedCode = redisTemplate.opsForValue().get(key);

    log.debug("Verification attempt - Email: {}, Input Code: {}, Saved Code: {}", email, code, savedCode);

    if (savedCode == null) {
      throw new ServiceException(ErrorCodes.EMAIL_VERIFICATION_NOT_FOUND);
    }

    if (!savedCode.equals(code)) {
      throw new ServiceException(ErrorCodes.EMAIL_VERIFICATION_NOT_MATCH);
    }

    redisTemplate.delete(key);
    redisTemplate.opsForValue()
            .set("email:verify:" + email, "verified", 3, TimeUnit.MINUTES);

    return true;
  }

  private void _sendEmail(EmailDto emailDto) {
    MimeMessage emailForm = _createEmailForm(emailDto);
    try {
      emailSender.send(emailForm);

    } catch (RuntimeException e) {
      throw new ServiceException(ErrorCodes.EMAIL_SEND_FAIL);
    }
  }

  private MimeMessage _createEmailForm(EmailDto emailDto) {
    try {
      MimeMessage message = emailSender.createMimeMessage();
      MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "UTF-8");
      messageHelper.setTo(emailDto.getReceiver());
      messageHelper.setFrom(EmailDto.SENDER_EMAIL, EmailDto.SENDER_NAME);
      messageHelper.setSubject(emailDto.getTitle());
      messageHelper.setText(emailDto.getText());

      emailSender.send(message);

      return message;
    } catch (MessagingException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private String _generateVerificationCode() {
    return String.format("%06d", secureRandom.nextInt(1000000));
  }

  public void _saveVerificationCode(String email, String code) {
    String key = KEY_PREFIX + email;

    redisTemplate.opsForValue()
        .set(key, code, Duration.ofMinutes(30));
  }

}
