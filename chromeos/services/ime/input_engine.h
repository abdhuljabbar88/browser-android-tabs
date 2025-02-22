// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef CHROMEOS_SERVICES_IME_INPUT_ENGINE_H_
#define CHROMEOS_SERVICES_IME_INPUT_ENGINE_H_

#include "chromeos/services/ime/public/mojom/input_engine.mojom.h"
#include "mojo/public/cpp/bindings/pending_receiver.h"
#include "mojo/public/cpp/bindings/pending_remote.h"
#include "mojo/public/cpp/bindings/receiver_set.h"

namespace chromeos {
namespace ime {

namespace rulebased {
class Engine;
}

class InputEngineContext {
 public:
  explicit InputEngineContext(const std::string& ime);
  ~InputEngineContext();

  std::string ime_spec;
  std::unique_ptr<rulebased::Engine> engine;

 private:
  DISALLOW_COPY_AND_ASSIGN(InputEngineContext);
};

// A basic implementation of InputEngine without using any decoder.
class InputEngine : public mojom::InputChannel {
 public:
  InputEngine();
  ~InputEngine() override;

  // Binds the mojom::InputChannel interface to this object and returns true if
  // the given ime_spec is supported by the engine.
  virtual bool BindRequest(const std::string& ime_spec,
                           mojo::PendingReceiver<mojom::InputChannel> receiver,
                           mojo::PendingRemote<mojom::InputChannel> remote,
                           const std::vector<uint8_t>& extra);

  // Returns whether the given ime_spec is supported by this engine.
  virtual bool IsImeSupported(const std::string& ime_spec);

  // mojom::MessageChannel overrides:
  void ProcessText(const std::string& message,
                   ProcessTextCallback callback) override;
  void ProcessMessage(const std::vector<uint8_t>& message,
                      ProcessMessageCallback callback) override;

  // TODO(https://crbug.com/837156): Implement a state for the interface.

 private:
  std::string Process(const std::string& message,
                      const InputEngineContext* context);

  mojo::ReceiverSet<mojom::InputChannel, std::unique_ptr<InputEngineContext>>
      channel_receivers_;

  DISALLOW_COPY_AND_ASSIGN(InputEngine);
};

}  // namespace ime
}  // namespace chromeos

#endif  // CHROMEOS_SERVICES_IME_INPUT_ENGINE_H_
