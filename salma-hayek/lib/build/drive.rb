#!/usr/bin/ruby -w

$PREVIOUS_VERBOSE = $VERBOSE
$VERBOSE = false

# Copyright (C) 2012 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
require 'google/apis/drive_v2'
require 'google/api_client/client_secrets'
require 'google/api_client/auth/storage'
require 'google/api_client/auth/storages/file_store'
require 'google/api_client/auth/installed_app'
require 'logger'

$VERBOSE = $PREVIOUS_VERBOSE

CREDENTIAL_STORE_FILE = "#{$0}-oauth2.json"

# Handles authentication and loading of the API.
def setup()
  log_file = File.open('drive.log', 'a+')
  log_file.sync = true
  logger = Logger.new(log_file)
  logger.level = Logger::DEBUG

  $PREVIOUS_VERBOSE = $VERBOSE
  $VERBOSE = false
  # FileStorage stores auth credentials in a file, so they survive multiple runs
  # of the application. This avoids prompting the user for authorization every
  # time the access token expires, by remembering the refresh token.
  # Note: FileStorage is not suitable for multi-user applications.
  file_storage = Google::APIClient::Storage.new(Google::APIClient::FileStore.new(CREDENTIAL_STORE_FILE))
  $VERBOSE = $PREVIOUS_VERBOSE
  if file_storage.authorize.nil?
    client_secrets = Google::APIClient::ClientSecrets.load
    # The InstalledAppFlow is a helper class to handle the OAuth 2.0 installed
    # application flow, which ties in with FileStorage to store credentials
    # between runs.
    flow = Google::APIClient::InstalledAppFlow.new(
      :client_id => client_secrets.client_id,
      :client_secret => client_secrets.client_secret,
      :scope => ['https://www.googleapis.com/auth/drive']
    )
    # Launchy 2.4.2 is broken on Cygwin64 (probably all of Windows) such that it runs
    # itself rather than the "start" command.
    # Even if I hack out the erroneous "launchy" argument from browser.rb (see the check-in comment),
    # start jibs at the URL for unspecified reasons, possibly related to quoting or length.
    # By turning on the debug, you get to see the requested URL, which you can copy and paste.
    # Once you click Accept in Chrome, everything else moves along automatically.
    ENV["LAUNCHY_DEBUG"] = "true"
    authorization = flow.authorize(file_storage)
  else
    authorization = file_storage.authorization
  end

  drive = Google::Apis::DriveV2::DriveService.new
  drive.authorization = authorization

  return drive
end

def insert_file(drive, title, description, parentId, mimeType, fileName)
  parent = Google::Apis::DriveV2::ParentReference.new(id: parentId)
  parents = [parent]
  file_object = Google::Apis::DriveV2::File.new(title: title, description: description, mime_type: mimeType, parents: parents)
  result = drive.insert_file(file_object, upload_source: fileName)
  #jj(result.data().to_hash())
  return result.id()
end

def find_file(drive, parentId, title)
  result = drive.list_children(parentId, q: "title='#{title}'")
  #jj(result.to_hash())
  items = result.items()
  if items == []
    return nil
  end
  if items.size() != 1
    raise("What is this Drive madness, with #{items.size()} files all called #{title.inspect()}?")
  end
  item = items[0]
  id = item.id()
  return id
end

def copy_file(drive, title, parentId, fileId)
  parent = Google::Apis::DriveV2::ParentReference.new(id: parentId)
  parents = [parent]
  file_object = Google::Apis::DriveV2::File.new(title: title, parents: parents)
  drive.copy_file(fileId, file_object)
  #jj(result.to_hash())
end

def update_file(drive, mimeType, fileId, fileName)
  file_object = Google::Apis::DriveV2::File.new(mime_type: mimeType)
  drive.update_file(fileId, file_object, upload_source: fileName)
  #jj(result.to_hash())
end

if __FILE__ == $0
  description = ARGV.shift()
  parentId = ARGV.shift()
  mimeType = ARGV.shift()
  fileName = ARGV.shift()
  latestDirectory = ARGV.shift()
  latestLink = ARGV.shift()
  if latestLink == nil || ARGV.empty?() == false
    raise("Syntax: drive.rb <description> <id of parent directory> <mime type> <filename> <latest directory> <latest link>")
  end
  title = fileName.sub(/^.*\//, "")
  drive = setup()
  if find_file(drive, parentId, title)
    $stderr.puts("#{title} is already there")
    exit(0)
  end
  fileId = insert_file(drive, title, description, parentId, mimeType, fileName)
  latestFile = find_file(drive, latestDirectory, latestLink)
  if latestFile
    update_file(drive, mimeType, latestFile, fileName)
  else
    copy_file(drive, latestLink, latestDirectory, fileId)
  end
end

# The first time you try to upload from a machine, you need to get salma-hayek/lib/build/client_secrets.json.
# Navigate to https://console.developers.google.com/apis/credentials?pli=1&project=jessies-downloads-uploader
# and select Native client 1.

# I hadn't uploaded from this machine for a few months when I got the following error.
# Removing salma-hayek/lib/build/drive.rb-oauth2.json is the first step to fix it.
# Then, on trying to make native-dist in eg terminator, you should get
# a browser pop to confirm access, modulo the caveat above re Launchy.
# /Users/mad/.gem/ruby/1.8/gems/signet-0.5.0/lib/signet/oauth_2/client.rb:885:in `fetch_access_token': Authorization failed.  Server message: (Signet::AuthorizationError)
# {
#  "error" : "invalid_grant"
# }
# 	from /Users/mad/.gem/ruby/1.8/gems/signet-0.5.0/lib/signet/oauth_2/client.rb:898:in `fetch_access_token!'
# 	from /Library/Ruby/Gems/1.8/gems/google-api-client-0.7.1/lib/google/api_client/auth/file_storage.rb:51:in `load_credentials'
# 	from /Library/Ruby/Gems/1.8/gems/google-api-client-0.7.1/lib/google/api_client/auth/file_storage.rb:46:in `open'
# 	from /Library/Ruby/Gems/1.8/gems/google-api-client-0.7.1/lib/google/api_client/auth/file_storage.rb:46:in `load_credentials'
# 	from /Library/Ruby/Gems/1.8/gems/google-api-client-0.7.1/lib/google/api_client/auth/file_storage.rb:39:in `initialize'
#  	from ./drive.rb:48:in `new'
# 	from ./drive.rb:48:in `setup'
#	from ./drive.rb:138
