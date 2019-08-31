package net.tiny.ws;

interface ControllerService {
	String REQEST_REGEX = "(^status|^start|^stop|^suspend|^resume)([&][0-9a-zA-Z_-]+)*";
	void setControllable(Controllable controller);
}
