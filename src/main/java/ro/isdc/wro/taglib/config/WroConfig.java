/*
* Copyright 2011, 2012 France Télécom
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* 
* Author is Julien Wajsberg <julien.wajsberg@orange.com>
*/

package ro.isdc.wro.taglib.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.io.FilenameUtils;

import ro.isdc.wro.http.support.ServletContextAttributeHelper;
import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.group.Group;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;

public class WroConfig {
	private static final String WRO_BASE_URL = "/wro/";

	private static WroConfig instance;

	private Map<String, FilesGroup> groups;
	private ServletContext servletContext;
	private boolean initialized = false;

	public static WroConfig getInstance() throws ConfigurationException {
		if (instance == null) {
			throw new ConfigurationException("The instance was not created.");
		}
		/* lazy initialization because we need to be in a request thread */
		synchronized(instance) {
			if (!instance.initialized) {
				instance.loadConfig();
				instance.loadMinimizedFiles();
				instance.initialized = true;
			}
		}
		return instance;
	}

	private void loadConfig() throws ConfigurationException {
		ServletContextAttributeHelper helper = new ServletContextAttributeHelper(servletContext);
		WroModel model = helper.getManagerFactory().create().getModelFactory().create();
		
		groups = new HashMap<String, FilesGroup>();
		
		for(Group group: model.getGroups()) {
			String groupName = group.getName();
			List<String> jsFiles = getFilesFor(group, ResourceType.JS);
			List<String> cssFiles = getFilesFor(group, ResourceType.CSS);
			FilesGroup filesGroup = new FilesGroup(groupName);
			filesGroup.put("js", jsFiles);
			filesGroup.put("css", cssFiles);
			groups.put(groupName, filesGroup);
		}
	}

	private List<String> getFilesFor(Group group, ResourceType resourceType) {
		List<String> result = new ArrayList<String>();
		Group filteredGroup = group.collectResourcesOfType(resourceType);
		for (Resource resource: filteredGroup.getResources()) {
			String file = resource.getUri();
			result.add(file);
		}

		return result;
	}

	private void loadMinimizedFiles() {
		
		@SuppressWarnings("unchecked")
		Set<String> resourcePaths = servletContext
				.getResourcePaths(WRO_BASE_URL);
		if (resourcePaths == null) {
			return;
		}

		for (String path : resourcePaths) {
			String basename = FilenameUtils.getBaseName(path);
			String groupName = basename.substring(0, basename.lastIndexOf('-'));
			if (groups.containsKey(groupName)) {
				String type = FilenameUtils.getExtension(path);
				FilesGroup group = groups.get(groupName);
				group.putMinimizedFile(type, path);
			}
		}
	}

	/* package */static synchronized void createInstance(ServletContext context) {
		if (instance == null) {
			instance = new WroConfig();
			instance.servletContext = context;
		}
	}

	public FilesGroup getGroup(String groupName) {
		if (initialized) {
			return groups.get(groupName);
		} else {
			throw new ConfigurationException(
					"WroConfig was not correctly initialized");
		}
	}
}
