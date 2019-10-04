package net.hurstfrost.hudson.sounds;

import hudson.model.User;
import hudson.security.AccessDeniedException2;
import hudson.util.FormValidation;
import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.PLAY_METHOD;
import net.hurstfrost.hudson.sounds.SoundsAgentAction.SoundsAgentActionDescriptor;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.http.Cookie;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SoundsAgentActionTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

	private SoundsAgentAction instance;
	private SoundsAgentActionDescriptor descriptor;
	private StaplerRequest request;
	private StaplerResponse response;
	private SecurityContext securityContext;

	@Before
	public void before() throws Exception {
		j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
		j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy());
		securityContext = SecurityContextHolder.getContext();

		System.out.println(securityContext.getAuthentication());

		descriptor = (SoundsAgentActionDescriptor) j.jenkins.getDescriptor("SoundsAgentAction");
		instance = new SoundsAgentAction();

		descriptor.version = 10;

		request = mock(StaplerRequest.class);
		response = mock(StaplerResponse.class);

		HudsonSoundsNotifier.HudsonSoundsDescriptor.getDescriptor().setPlayMethod(PLAY_METHOD.BROWSER);
	}

	@Test
	public void doTestSoundWhenAuthenticated() {
		FormValidation formValidation = descriptor.doTestSound("YAWN");
	}

	@Test
	public void doTestSoundWhenNotAuthenticated() {
		securityContext.setAuthentication(User.getOrCreateByIdOrFullName("nopermissions").impersonate());

		try {
			FormValidation formValidation = descriptor.doTestSound("YAWN");

			fail("Should have been denied.");
		} catch (Exception e) {
		}
	}

	@Test
	public void doTestUrlWhenNotAuthenticated() {
		securityContext.setAuthentication(User.getOrCreateByIdOrFullName("nopermissions").impersonate());

		try {
			FormValidation formValidation = descriptor.doTestUrl("YAWN");

			fail("Should have been denied.");
		} catch (Exception e) {
		}
	}

	@Test
	public void doCancelSounds() {
		// given:
		when(request.getCookies()).thenReturn(new Cookie[0]);
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);

		// when:
		instance.doCancelSounds();
		JSONHttpResponse jsonResponse = instance.doGetSounds(request, response, 10);
		
		// then:
		assertFalse(jsonResponse.jsonObject.containsKey("play"));
		assertEquals(13, jsonResponse.jsonObject.optLong("v"));
		assertTrue(jsonResponse.jsonObject.optBoolean("x"));

		verify(request, times(2)).getCookies();
		verify(response).addCookie((Cookie) anyObject());
		verify(response).setContentType("application/json");
		verifyNoMoreInteractions(request, response);
	}

	@Test
	public void doCancelSoundsWithNotAuthenticated() {
		securityContext.setAuthentication(User.getOrCreateByIdOrFullName("nopermissions").impersonate());

		try {
			instance.doCancelSounds();

			fail("Should have been denied.");
		} catch (AccessDeniedException2 e) {
		}
	}

	@Test
	public void doGetSoundsWithNoVersionParameterOrCookie() throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[0]);
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);

		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, null);

		assertEquals(12, jsonResponse.jsonObject.getInt("v"));
		assertNull(jsonResponse.jsonObject.opt("play"));

		verify(request, times(2)).getCookies();
		verify(response).addCookie((Cookie) anyObject());
		verify(response).setContentType("application/json");
		verifyNoMoreInteractions(request, response);
	}

	@Test
	public void doGetSoundsWithCookieButNoVersionParameter() throws Exception {
		Cookie cookie = new Cookie("SoundsAgentActionDescriptorVersion", "10");
		when(request.getCookies()).thenReturn(new Cookie[] { cookie });
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);

		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, null);

		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));

		verify(request, times(3)).getCookies();
		verify(response).addCookie((Cookie) anyObject());
		verify(response).setContentType("application/json");
		verifyNoMoreInteractions(request, response);
	}

	@Test
	public void doGetSoundsReturnsNoPlayedSounds() throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[0]);
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 12);
		
		assertEquals(12, jsonResponse.jsonObject.getInt("v"));
		assertNull(jsonResponse.jsonObject.opt("play"));

		verify(request, times(2)).getCookies();
		verify(response).addCookie((Cookie) anyObject());
		verify(response).setContentType("application/json");
		verifyNoMoreInteractions(request, response);
	}

	@Test
	public void doGetSoundsReturnsUnplayedSounds() throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[0]);
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 10);
		
		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));

		verify(request, times(2)).getCookies();
		verify(response).addCookie((Cookie) anyObject());
		verify(response).setContentType("application/json");
		verifyNoMoreInteractions(request, response);
	}

	@Test
	public void doGetSoundsReturnsMutedWhenMutedLocally() {
		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(SoundsAgentAction.MUTE_COOKIE_NAME, "muted") });
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 10);
		
		assertEquals(-1, jsonResponse.jsonObject.getInt("v"));
		assertFalse(jsonResponse.jsonObject.containsKey("play"));
		assertEquals(60000, jsonResponse.jsonObject.getInt("p"));

		verify(request, times(2)).getCookies();
		verify(response).addCookie((Cookie) anyObject());
		verify(response).setContentType("application/json");
		verifyNoMoreInteractions(request, response);
	}

	@Test
	public void doGetSoundsHasSyncDelay() throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[0]);
		descriptor.addSound("sound1", null);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 10);

		int	expectedDelay = 2200;
		
		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));
		assertTrue(String.format("d=%d", jsonResponse.jsonObject.getLong("d")), expectedDelay >= jsonResponse.jsonObject.getLong("d") && jsonResponse.jsonObject.getLong("d") > expectedDelay-100);

		verify(request, times(2)).getCookies();
		verify(response, times(1)).addCookie((Cookie) anyObject());
		verify(response, times(1)).setContentType("application/json");

		Thread.sleep(500);
		expectedDelay -= 500;
		
		jsonResponse = instance.doGetSounds(request, response, 10);
		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));
		assertTrue(String.format("d=%d", jsonResponse.jsonObject.getLong("d")), expectedDelay >= jsonResponse.jsonObject.getLong("d") && jsonResponse.jsonObject.getLong("d") > expectedDelay-100);

		verify(request, times(4)).getCookies();
		verify(response, times(2)).addCookie((Cookie) anyObject());
		verify(response, times(2)).setContentType("application/json");

		Thread.sleep(500);
		expectedDelay -= 500;
		
		jsonResponse = instance.doGetSounds(request, response, 10);
		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));
		assertTrue(String.format("d=%d", jsonResponse.jsonObject.getLong("d")), expectedDelay >= jsonResponse.jsonObject.getLong("d") && jsonResponse.jsonObject.getLong("d") > expectedDelay-100);

		verify(request, times(6)).getCookies();
		verify(response, times(3)).addCookie((Cookie) anyObject());
		verify(response, times(3)).setContentType("application/json");
		verifyNoMoreInteractions(request, response);
	}

	@Test
	public void doGetSoundsInImmediateAreNotDelayed() throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[0]);
		descriptor.addSound("sound1", 0);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 10);
		
		assertEquals(0, jsonResponse.jsonObject.optLong("d"));

		verify(request, times(2)).getCookies();
		verify(response).addCookie((Cookie) anyObject());
		verify(response).setContentType("application/json");
		verifyNoMoreInteractions(request, response);
	}

	@Test
	public void soundAtOffset() throws Exception {
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		assertNull(descriptor.soundAtOffset(9));
		assertEquals("sound1", descriptor.soundAtOffset(10).getUrl(null));
		assertEquals("sound2", descriptor.soundAtOffset(11).getUrl(null));
		assertNull(descriptor.soundAtOffset(12));
	}

	@Test
	public void soundsAreRemovedAfter5s() throws Exception {
		descriptor.addSound("sound1", 0);
		Thread.sleep(1000);
		descriptor.addSound("sound2", 0);
		
		assertEquals("sound1", descriptor.soundAtOffset(10).getUrl(null));
		assertEquals("sound2", descriptor.soundAtOffset(11).getUrl(null));
		assertEquals(10, descriptor.version);
		assertEquals(2, descriptor.wavsToPlay.size());
		
		Thread.sleep(SoundsAgentAction.SOUND_QUEUE_EXPIRATION_PERIOD_MS - 1000 + 100);
		// After just over 4s sound1 should still be in the list, but expired
		assertEquals("sound1", descriptor.soundAtOffset(10).getUrl(null));
		assertTrue(descriptor.soundAtOffset(10).expired(0));
		// sound2 should be valid
		assertEquals("sound2", descriptor.soundAtOffset(11).getUrl(null));
		assertFalse(descriptor.soundAtOffset(11).expired(0));
		assertEquals(10, descriptor.version);
		assertEquals(2, descriptor.wavsToPlay.size());
		
		Thread.sleep(SoundsAgentAction.EXPIRY_EXTENSION);
		// Now sound1 should have gone
		assertNull(descriptor.soundAtOffset(10));
		assertEquals(11, descriptor.version);
		assertEquals(1, descriptor.wavsToPlay.size());
		
		Thread.sleep(1000);
		assertNull(descriptor.soundAtOffset(12));
		assertEquals(0, descriptor.wavsToPlay.size());
		assertEquals(12, descriptor.version);
	}

	/*
	@Test
	public void testAudioFormats() throws Exception {
		String[]	urls = new String[] {
			"file:///System/Library/Sounds/Tink.aiff",
			"file:///Users/ed/Desktop/atttts.wav",
			"file:///Users/ed/Desktop/bella.wav",
		};
		
		for (String url : urls) {
			AudioInputStream source = null;
			try {
				source = AudioSystem.getAudioInputStream(new URL(url).openStream());
			} catch (UnsupportedAudioFileException e) {
				// Invalid format
			}
			System.out.println(url + " : " + (source!=null?source.getFormat():"null"));
			
			try {
				Info info = new DataLine.Info(SourceDataLine.class, source.getFormat());
				SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

				line.open();
				line.start();
				byte[]	buffer = new byte[8096];
				while (true) {
					int read = source.read(buffer);
					if (read <= 0) break;
					line.write(buffer, 0, read);
				}
				line.drain();
				line.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testPlaySound() throws Exception {
		InputStream inputStream = new URL("file:///System/Library/Sounds/Tink.aiff").openStream();
		instance.playSound(inputStream, true);
		
		response.addCookie((Cookie) anyObject());
		
		replay(request, response);
		JSONHttpResponse sound = instance.doGetSounds(request, response, 10);
		System.out.println("play="+sound.jsonObject.getString("play"));
		verify(request, response);
	}*/
}
