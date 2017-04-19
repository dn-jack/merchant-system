var a = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
function h(r) {
	if (r) {
		r = c(r);
		var e = new RegExp("=", "g");
		return r = r.replace(e, ""), r = r.split("").reverse().join("")
	}
}

function c(r) {
	var e, t, o, c, i, h;
	for (o = r.length, t = 0, e = ""; o > t;) {
		if (c = 255 & r.charCodeAt(t++), t == o) {
			e += a.charAt(c >> 2), e += a.charAt((3 & c) << 4), e += "==";
			break
		}
		if (i = r.charCodeAt(t++), t == o) {
			e += a.charAt(c >> 2), e += a.charAt((3 & c) << 4 | (240 & i) >> 4), e += a
					.charAt((15 & i) << 2), e += "=";
			break
		}
		h = r.charCodeAt(t++), e += a.charAt(c >> 2), e += a
				.charAt((3 & c) << 4 | (240 & i) >> 4), e += a
				.charAt((15 & i) << 2 | (192 & h) >> 6), e += a.charAt(63 & h)
	}
	return e
}