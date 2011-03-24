import com.testapp.TestUser

class HackController {

	def userCache

	def getSessionValue = {
		def value = session[params.name]
		render value ? value.toString() : ''
	}

	def getSessionNames = {
		session.nowdate = new Date() // to test it's working

		def sb = new StringBuilder()
		session.attributeNames.each { String name ->
			sb.append name
			sb.append '<br/>\n'
		}
		render sb.toString()
	}

	def getUserProperty = {
		render TestUser.findByUsername(params.user)."$params.propName"
	}

	def setUserProperty = {
		TestUser.findByUsername(params.user).properties = params
		userCache.removeUserFromCache params.user
		render 'ok'
	}

	def clearAllData = {
		render 'ok'
	}
}
