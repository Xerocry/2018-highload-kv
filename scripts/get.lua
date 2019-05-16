cnt = 0
method = "GET"

request = function()
    local path = "/v0/entity?id=query" .. cnt
    cnt = cnt + 1
    return wrk.format(method, path, nil, nil)
end