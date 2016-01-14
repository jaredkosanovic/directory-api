package edu.oregonstate.mist.directoryapi

import edu.oregonstate.mist.api.Resource
import edu.oregonstate.mist.api.AuthenticatedUser
import io.dropwizard.auth.Auth
import org.ldaptive.LdapException
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.ResponseBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.UriInfo

/**
 * Directory entity resource class.
 */
@Path('/directory')
class DirectoryEntityResource extends Resource {
    private final DirectoryEntityDAO directoryEntityDAO
    private final String RESOURCETYPE = "directory"

    // TODO: Move to skeleton or configuration file
    public static final Integer DEFAULT_PAGE_NUMBER = 1
    public static final Integer DEFAULT_PAGE_SIZE = 10

    @Context
    UriInfo uriinfo

    /**
     * Constructs the object after receiving and storing directoryEntityDAO instance.
     *
     * @param directoryEntityDAO
     */
    public DirectoryEntityResource(DirectoryEntityDAO directoryEntityDAO) {
        this.directoryEntityDAO = directoryEntityDAO
    }

    /**
     * Responds to GET requests by returning array of resultObject objects matching search query parameter.
     *
     * @param authenticatedUser
     * @param searchQuery
     * @return resultObject object
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBySearchQuery(
            @Auth AuthenticatedUser authenticatedUser,
            @QueryParam('q') String searchQuery) {
        ResponseBuilder responseBuilder
        if (!searchQuery) {
            responseBuilder = badRequest('Missing query parameter.')
        } else {
            try {
                List<DirectoryEntity> directoryEntityList = directoryEntityDAO.getBySearchQuery(searchQuery)
                List<ResourceObject> resourceObjectList = new ArrayList<ResourceObject>()
                directoryEntityList.each {
                    resourceObjectList.add(new ResourceObject(
                            id: it.osuuid,
                            type: RESOURCETYPE,
                            attributes: it)
                    )
                }
                ResultObject resultObject = new ResultObject(
                        links: null,
                        data: resourceObjectList
                )
                responseBuilder = ok(resultObject)
            } catch (LdapException ldapException) {
                responseBuilder = internalServerError(ldapException.message)
            }
        }
        responseBuilder.build()
    }

    /**
     * Responds to GET requests by returning resultObject object matching argument id.
     *
     * @param authenticatedUser
     * @param osuuid
     * @return resultObject object
     */
    @GET
    @Path('/{osuuid: \\d+}')
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByOSUUID(
            @Auth AuthenticatedUser authenticatedUser,
            @PathParam('osuuid') Long osuuid) {
        ResponseBuilder responseBuilder
        try {
            DirectoryEntity directoryEntity = directoryEntityDAO.getByOSUUID(osuuid)
            if (directoryEntity != null) {
                ResourceObject resourceObject = new ResourceObject(
                        id: osuuid,
                        type: RESOURCETYPE,
                        attributes: directoryEntity
                )
                ResultObject resultObject = new ResultObject(
                        links: null,
                        data: resourceObject
                )
                responseBuilder = ok(resultObject)
            } else {
                responseBuilder = notFound()
            }
        } catch (LdapException ldapException) {
            responseBuilder = internalServerError(ldapException.message)
        }
        responseBuilder.build()
    }

    /**
     * Add pagination links to data search results.
     *
     * @param topLevelHits      First "hits"
     * @param q
     * @param type
     * @param resultObject
     */
    private void setPaginationLinks(Integer topLevelHits, String q, String type, ResultObject resultObject) {

        if (!topLevelHits) {
            return
        }

        Integer pageNumber = getPageNumber()
        Integer pageSize = getPageSize()
        def map = ["q": q, "type": type, "pageSize": pageSize, "pageNumber": pageNumber]
        int lastPage = Math.ceil(topLevelHits / pageSize)

        resultObject.links["self"] = getPaginationUrl(map)
        map.pageNumber = 1
        resultObject.links["first"] = getPaginationUrl(map)
        map.pageNumber = lastPage
        resultObject.links["last"] = getPaginationUrl(map)

        if (pageNumber > DEFAULT_PAGE_NUMBER) {
            map.pageNumber = pageNumber - 1
            resultObject.links["prev"] = getPaginationUrl(map)
        } else {
            resultObject.links["prev"] = null
        }

        if (topLevelHits > (pageNumber * pageSize)) {
            map.pageNumber = pageNumber + 1
            resultObject.links["next"] = getPaginationUrl(map)
        } else {
            resultObject.links["next"] = null
        }
    }

    /**
     * Returns string url for use in pagination links.
     *
     * @param params
     * @return
     */
    private String getPaginationUrl(def params) {
        def uriAndPath = uriinfo.getBaseUri().toString() + uriinfo.getPath()
        def nonNullParams = params.clone()
        nonNullParams["page[number]"] = nonNullParams['pageNumber']
        nonNullParams["page[size]"] = nonNullParams['pageSize']
        nonNullParams.remove('pageSize')
        nonNullParams.remove('pageNumber')

        nonNullParams = nonNullParams.findAll() { it.value } .collect { k, v -> "$k=$v"}
        uriAndPath + "?" + nonNullParams.join('&')
    }

    /**
     * Returns value for an array parameter in GET string.
     *
     * @param key
     * @param index
     * @param queryParameters
     * @return
     */
    public static String getArrayParameter(String key, String index, MultivaluedMap<String, String> queryParameters) {
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            if (!entry.key.contains("[") && !entry.key.contains("]")) {
                continue
            }

            int a = entry.key.indexOf('[')
            int b = entry.key.indexOf(']')

            if (entry.key.substring(0, a).equals(key)) {
                if (entry.key.substring(a + 1, b).equals(index)) {
                    return entry.value.get(0)
                }
            }
        }

        null
    }

    /**
     * Returns page number used by pagination.
     *
     * @return
     */
    private Integer getPageNumber() {
        def pageNumber = getArrayParameter("page", "number", uriinfo.getQueryParameters())
        if (!pageNumber || !pageNumber.isInteger()) {
            return DEFAULT_PAGE_NUMBER
        }

        pageNumber.toInteger()
    }

    /**
     * Returns the page size used by pagination.
     *
     * @return
     */
    private Integer getPageSize() {
        def pageSize = getArrayParameter("page", "size", uriinfo.getQueryParameters())
        if (!pageSize || !pageSize.isInteger()) {
            return DEFAULT_PAGE_SIZE
        }

        pageSize.toInteger()
    }
}
