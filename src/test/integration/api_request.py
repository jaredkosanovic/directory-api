import json, requests

def basic_request(url, access_token, verb="get"):
    headers = {'Authorization': access_token}
    request = requests.request(verb, url, headers=headers)
    return request