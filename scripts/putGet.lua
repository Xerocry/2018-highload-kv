math.randomseed(os.time())

cnt = 0

request = function()
    local path = "/v0/entity?id=query" .. cnt
    local body = nil

    if (cnt % 2 == 0) then
        method = "PUT"
        body = "data"
        for i=1,math.random(100, 200) do
            body = body .. math.random(1000, 9999)
        end
        wrk.body = body
    else
        method = "GET"
    end

    cnt = cnt + 1;
    return wrk.format(method, path, nil, body)
end