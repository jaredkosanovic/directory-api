package edu.oregonstate.mist.directoryapi

import org.ldaptive.DefaultConnectionFactory
import org.ldaptive.Connection
import org.ldaptive.LdapEntry
import org.ldaptive.LdapException
import org.ldaptive.Response
import org.ldaptive.ResultCode
import org.ldaptive.SearchOperation
import org.ldaptive.SearchRequest
import org.ldaptive.SearchResult
import org.ldaptive.pool.PooledConnectionFactory
import org.ldaptive.pool.SoftLimitConnectionPool
import org.ldaptive.pool.PoolConfig
import java.util.regex.Pattern

/**
 * Directory entity data access object.
 */
class DirectoryEntityDAO {
    private final String LDAP_URL
    private final String BASE_DN
    private final PooledConnectionFactory pooledConnectionFactory
    private static final Pattern illegalCharacterPattern = Pattern.compile(
            '''(?x)             # this extended regex defines
               (?!              # any character that is not
                  [
                   \\p{IsLatin} # a unicode letter,
                   0-9          # a number,
                   -            # a hyphen,
                   _            # an underscore,
                   \\.          # a period, or
                   @            # an at sign
                  ])
               .                # to be an illegal character.
            ''')
    private static final Pattern spacesPattern = Pattern.compile(' +')
    private static final Pattern dollarSignPattern = Pattern.compile('\\$')

    /** Constructs a directory entity data access object with no LDAP configuration (for unit testing).
     */
    public DirectoryEntityDAO() { }

    /**
     * Constructs the directory entity data access object with given LDAP configuration.
     *
     * @param ldapConfiguration
     */
    public DirectoryEntityDAO(Map<String,Object> ldapConfiguration) {
        LDAP_URL = (String)ldapConfiguration.get('url')
        BASE_DN = (String)ldapConfiguration.get('base')
        DefaultConnectionFactory defaultConnectionFactory = new DefaultConnectionFactory(LDAP_URL)
        PoolConfig poolConfig = new PoolConfig(
                maxPoolSize: (int)ldapConfiguration.get('maxPoolSize'),
                minPoolSize: (int)ldapConfiguration.get('minPoolSize'),
                validateOnCheckIn: (boolean)ldapConfiguration.get('validateOnCheckIn'),
                validateOnCheckOut: (boolean)ldapConfiguration.get('validateOnCheckOut'),
                validatePeriod: (long)ldapConfiguration.get('validatePeriod'),
                validatePeriodically: (boolean)ldapConfiguration.get('validatePeriodically')
        )
        SoftLimitConnectionPool pool = new SoftLimitConnectionPool(
                poolConfig, defaultConnectionFactory
        )

        pool.initialize()
        pooledConnectionFactory = new PooledConnectionFactory(pool)
    }

    /**
     * Returns list of directory entities matching search query.
     *
     * @param searchQuery
     * @return list of directory entities
     * @throws LdapException
     */
    public List<DirectoryEntity> getBySearchQuery(String searchQuery)
            throws LdapException {
        String filter = '(&(objectclass=person)'
        for (String searchTerm : split(sanitize(searchQuery))) {
            if (searchTerm) {
                filter += '(|' + '(cn=*' + searchTerm + '*)' +
                                '(uid=' + searchTerm + ')' +
                               '(mail=*' + searchTerm + '*)' + ')'
            }
        }
        filter += ')'
        searchLDAP(filter)
    }

    /**
     * Returns directory entity matching input id.
     *
     * @param osuuid
     * @return directory entity
     * @throws LdapException
     */
    public DirectoryEntity getByOSUUID(Long osuuid)
            throws LdapException {
        String filter = '(osuuid=' + osuuid + ')'
        List<DirectoryEntity> result = searchLDAP(filter)
        if (result) {
            return result.get(0)
        } else {
            return null
        }
    }

    /**
     * Sanitizes the search query string by replacing illegal characters with spaces.
     *
     * @param searchQuery
     * @return sanitized search query
     */
    private static String sanitize(String searchQuery) {
        illegalCharacterPattern.matcher(searchQuery).replaceAll(' ')
    }

    /**
     * Splits the search query string delimited by spaces.
     *
     * @param string
     * @return
     */
    private static String[] split(String string) {
        spacesPattern.split(string)
    }

    /**
     * Searches LDAP directory for filter string.
     *
     * @param filter
     * @return directoryEntityList
     */
    private List<DirectoryEntity> searchLDAP(String filter)
            throws LdapException {
        List<DirectoryEntity> directoryEntityList = new ArrayList<DirectoryEntity>()
        Connection connection = pooledConnectionFactory.getConnection()
        try {
            SearchOperation operation = new SearchOperation(connection)
            SearchRequest request = new SearchRequest(BASE_DN, filter)
            Response<SearchResult> response = operation.execute(request)
            if (response.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                throw new LdapException('Size limit exceeded.', ResultCode.SIZE_LIMIT_EXCEEDED)
            } else {
                SearchResult result = response.getResult()
                for (LdapEntry entry : result.getEntries()) {
                    directoryEntityList.add(convert(entry))
                }
            }
        } finally {
            connection.close() // return connection to pool
        }
        directoryEntityList
    }

    /**
     * Converts LDAP entry to directory entity.
     *
     * @param ldapEntry
     * @return directoryEntity
     */
    private static DirectoryEntity convert(LdapEntry ldapEntry) {
        new DirectoryEntity(
                firstName:                get(ldapEntry, 'givenname'),
                lastName:                 get(ldapEntry, 'sn'),
                fullName:                 get(ldapEntry, 'cn'),
                primaryAffiliation:       affiliation(get(ldapEntry, 'osuprimaryaffiliation')),
                jobTitle:                 get(ldapEntry, 'title'),
                department:               get(ldapEntry, 'osudepartment'),
                departmentMailingAddress: address(get(ldapEntry, 'postaladdress')),
                homePhoneNumber:          get(ldapEntry, 'homephone'),
                homeAddress:              address(get(ldapEntry, 'homepostaladdress')),
                officePhoneNumber:        get(ldapEntry, 'telephonenumber'),
                officeAddress:            address(get(ldapEntry, 'osuofficeaddress')),
                faxNumber:                get(ldapEntry, 'facsimiletelephonenumber'),
                emailAddress:             get(ldapEntry, 'mail'),
                username:                 get(ldapEntry, 'uid'),
                osuuid:                   getOSUUid(ldapEntry)
        )
    }

    /**
     * Gets the named attribute of LDAP entry.
     *
     * @param ldapEntry
     * @param name
     * @return attribute value
     */
    private static String get(LdapEntry ldapEntry, String name) {
        ldapEntry.getAttribute(name)?.getStringValue()
    }

    /**
     * Gets an osuuid as a long
     *
     * @param ldapEntry
     * @return Long osuuid
     */
    private static Long getOSUUid(LdapEntry ldapEntry) {
        String osuuidString = get(ldapEntry, 'osuuid')
        Long osuuid = 0

        if (osuuidString && osuuidString.matches("[0-9]+")) {
            osuuid = Long.parseLong(osuuidString)
        }

        osuuid
    }

    /**
     * Expands affiliation abbreviation.
     *
     * @param abbreviation
     * @return affiliation
     */
    private static String affiliation(String abbreviation) {
        switch (abbreviation) {
            case 'E':
                return 'Employee'
            case 'S':
                return 'Student'
            case 'O':
                return 'Other'
            case 'U':
                return 'Unknown'
            default:
                null
        }
    }

    /**
     * Formats mailing address.
     *
     * @param address
     * @return formatted address
     */
    private static String address(String address) {
        if (address) {
            dollarSignPattern.matcher(address).replaceAll('\n')
        }
    }
}
