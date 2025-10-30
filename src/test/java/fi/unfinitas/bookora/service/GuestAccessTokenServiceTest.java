package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.model.Booking;
import fi.unfinitas.bookora.domain.model.GuestAccessToken;
import fi.unfinitas.bookora.exception.InvalidTokenException;
import fi.unfinitas.bookora.exception.TokenExpiredException;
import fi.unfinitas.bookora.repository.GuestAccessTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestAccessTokenServiceTest {

    @Mock
    private GuestAccessTokenRepository tokenRepository;

    @Mock
    private BookoraProperties bookoraProperties;

    @InjectMocks
    private GuestAccessTokenService tokenService;

    private Booking testBooking;
    private GuestAccessToken testToken;
    private UUID tokenUUID;
    private LocalDateTime bookingEndTime;

    @BeforeEach
    void setUp() {
        testBooking = mock(Booking.class);
        tokenUUID = UUID.randomUUID();
        bookingEndTime = LocalDateTime.now().plusDays(2).plusHours(1);

        lenient().when(testBooking.getEndTime()).thenReturn(bookingEndTime);

        final BookoraProperties.Guest guest = mock(BookoraProperties.Guest.class);
        final BookoraProperties.Guest.Token token = mock(BookoraProperties.Guest.Token.class);
        lenient().when(bookoraProperties.getGuest()).thenReturn(guest);
        lenient().when(guest.getToken()).thenReturn(token);
        lenient().when(token.getExpirationExtensionDays()).thenReturn(30);

        testToken = GuestAccessToken.builder()
                .token(tokenUUID)
                .booking(testBooking)
                .expiresAt(bookingEndTime.plusDays(30))
                .build();
    }

    @Test
    @DisplayName("Should generate token successfully")
    void shouldGenerateTokenSuccessfully() {
        when(tokenRepository.save(any(GuestAccessToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final GuestAccessToken result = tokenService.generateToken(testBooking);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getBooking()).isEqualTo(testBooking);
        assertThat(result.getExpiresAt()).isEqualTo(bookingEndTime.plusDays(30));

        verify(tokenRepository).save(any(GuestAccessToken.class));
    }

    @Test
    @DisplayName("Should validate token successfully")
    void shouldValidateTokenSuccessfully() {
        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(testToken));

        final GuestAccessToken result = tokenService.validateToken(tokenUUID);

        assertThat(result).isEqualTo(testToken);
        assertThat(result.getBooking()).isEqualTo(testBooking);
        verify(tokenRepository).findByToken(tokenUUID);
    }

    @Test
    @DisplayName("Should throw exception when token not found")
    void shouldThrowExceptionWhenTokenNotFound() {
        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.validateToken(tokenUUID))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token not found");

        verify(tokenRepository).findByToken(tokenUUID);
    }

    @Test
    @DisplayName("Should throw exception when token is expired")
    void shouldThrowExceptionWhenTokenIsExpired() {
        final GuestAccessToken expiredToken = GuestAccessToken.builder()
                .token(tokenUUID)
                .booking(testBooking)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> tokenService.validateToken(tokenUUID))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");

        verify(tokenRepository).findByToken(tokenUUID);
    }
}
