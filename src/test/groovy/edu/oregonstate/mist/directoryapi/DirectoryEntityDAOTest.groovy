package edu.oregonstate.mist.directoryapi

import io.dropwizard.testing.junit.DropwizardAppRule
import io.dropwizard.testing.ResourceHelpers
import org.ldaptive.LdapException
import org.ldaptive.ResultCode
import org.junit.Test
import org.junit.ClassRule
import org.junit.BeforeClass
import static org.junit.Assert.*

class DirectoryEntityDAOTest {
    private static final directoryEntityDAO = new DirectoryEntityDAO()
    private static final Long goodOSUUID = 51646559347
    private static final Long badOSUUID = 0
    private static final String goodUID = 'browtayl'
    private static final String badUID = 'abcdef'
    private static final String goodUnicodeUID = 'Daniel Gilberto'
    private static final String goodUnicodeSearchTerm = 'Hernández'
    private static final String overlyBroadSearchTerm = 'John'
    private static final String goodFirstName = 'Taylor'
    private static final String goodLastName = 'Brown'


    /*
    @Test
    public void testGetByOSUUID() {
        assertNotNull(directoryEntityDAO.getByOSUUID(goodOSUUID))
        assertNull(directoryEntityDAO.getByOSUUID(badOSUUID))
    }

    @Test
    public void testGetBySearchQuery() {
        assertTrue(directoryEntityDAO.getBySearchQuery(badUID).isEmpty())
        assertFalse(directoryEntityDAO.getBySearchQuery(goodUID).isEmpty())
    }

    @Test
    public void testGetBySearchQuerySplit() {
        assertFalse(directoryEntityDAO.getBySearchQuery(
                goodFirstName + ' ' + goodLastName).isEmpty())

        assertFalse(directoryEntityDAO.getBySearchQuery(
                goodLastName + ' ' + goodFirstName).isEmpty())
    }

    @Test
    public void testGetBySearchQuerySanitize() {
        List<DirectoryEntity> expected = directoryEntityDAO.getBySearchQuery(goodUID)
        assertEquals(expected, directoryEntityDAO.getBySearchQuery(goodUID + '('))
        assertEquals(expected, directoryEntityDAO.getBySearchQuery(goodUID + ')'))
        assertEquals(expected, directoryEntityDAO.getBySearchQuery(goodUID + '*'))
        assertEquals(expected, directoryEntityDAO.getBySearchQuery(goodUID + '&'))
        assertEquals(expected, directoryEntityDAO.getBySearchQuery(goodUID + '#'))
        assertEquals(expected, directoryEntityDAO.getBySearchQuery('#' + goodUID))
    }

    @Test
    public void testGetBySearchQuerySanitizeUnicode() {
        List<DirectoryEntity> expected = directoryEntityDAO.getBySearchQuery(goodUnicodeUID)
        assertEquals(expected, directoryEntityDAO.getBySearchQuery(goodUnicodeSearchTerm))
    }

    @Test
    public void testGetBySearchQuerySizeLimitExceeded() {
        try {
            directoryEntityDAO.getBySearchQuery(overlyBroadSearchTerm)
        } catch (LdapException ldapException) {
            assertTrue(ldapException.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED)
            return
        }
        fail()
    }

    @Test
    public void testGetBySearchQuerySizeLimit() {
        try {
            directoryEntityDAO.getBySearchQuery(goodFirstName + ' ' + goodLastName)
        } catch (LdapException ldapException) {
            if (ldapException.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                fail()
            }
        }
    }
    */

    @Test
    public void testSanitize() {
        assert directoryEntityDAO.sanitize(goodUID + "(").trim() == goodUID
        assert directoryEntityDAO.sanitize(goodUID + ")").trim() == goodUID
        assert directoryEntityDAO.sanitize(goodUID + "*").trim() == goodUID
        assert directoryEntityDAO.sanitize(goodUID + "&").trim() == goodUID
        assert directoryEntityDAO.sanitize(goodUID + "#").trim() == goodUID
        assert directoryEntityDAO.sanitize("#" + goodUID).trim() == goodUID
        assert directoryEntityDAO.sanitize(goodUnicodeSearchTerm) == goodUnicodeSearchTerm
    }
}
